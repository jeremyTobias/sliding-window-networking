import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PageRequestHandler extends Thread {	
	private final RrqPacket rrqPack;

	private PageReader pageReader;
	private URL url;	
	
	private boolean useDrop;
	private boolean useSliding;

	public PageRequestHandler(RrqPacket rrqPack, boolean useSliding, boolean useDrop) {
		this.rrqPack = rrqPack;
		this.useSliding = useSliding;
		this.useDrop = useDrop;
	}

	public void run() {
		// try to set the URL
		try {
			this.url = new URL(rrqPack.getURL());
		} catch (MalformedURLException e) {
			System.out.println(rrqPack.getURL() + " is an invalid URL.");
			return;
		}

		// try to create a connection to the URL so that we can read it's contents
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(this.url.openConnection().getInputStream());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException io) {
			System.out.println("Could not get page.");
			return;
		}

		// create a new TftpInit
		TftpInit tInit = null;
		int connectRetries = 0;
		while (tInit == null) {
			try {
				tInit = new TftpInit();
			} catch (SocketException e) {
				if (connectRetries >= 10) {
					System.out.println("Too many retries.");
					return;
				}
				connectRetries++;
			}
		}

		ArrayList<byte[]> payloads = new ArrayList<byte[]>();

		boolean finished = false;

		// fill a byte buffer and place in an array list, to be turned into a TFTP data packet later
		while(!finished) {
			ByteBuffer byteBuf = ByteBuffer.allocate(DataPacket.MAX_DATA);

			// while there's still space in the byte buffer and while there's still data, fill the byte buffer
			while (byteBuf.hasRemaining()) {
				byte[] data = new byte[1];
				try {
					int readData = in.read(data); // reading one byte at a time
					if (readData == -1) { // no more data, leave
						finished = true;
						break;
					} else {
						byteBuf.put(data); // place data in byte buffer
					}
				} catch (IOException io) {
					io.printStackTrace();
				}
			}

			/*
			* If the byte buffer is smaller than 512, we've reached the last packet
			* resize the payload and place in the array list
			* Else continue as normal
			*/
			if ((byteBuf.capacity() - byteBuf.remaining()) < DataPacket.MAX_DATA) {
				byte[] smallPayload = new byte[(byteBuf.capacity() - byteBuf.remaining())];
				byteBuf.rewind();
				byteBuf.get(smallPayload);
				payloads.add(smallPayload);
			} else { 
				byte[] payload = new byte[DataPacket.MAX_DATA];
				byteBuf.rewind();
				byteBuf.get(payload);
				payloads.add(payload);
			}
		}

		// get client info
		int clientPort = rrqPack.getPort();
		InetAddress clientAddress = rrqPack.getAddress();

		// if using sliding window, perform sliding window operations, else use sequential
		if (useSliding) {
			/*
			* Sliding window operations
			* Inital window setup
			*/
			int retries = 0;
			int packetIndex = 0;
			int windowSize = 4;
			int lastPacketBlockNumber = -1;
			boolean lastWindow = false;

			while (packetIndex < payloads.size()) {
				// window
				for (int i = 0; i < windowSize; i++) {
					// reached the end of the payload, exit
					if (((packetIndex * windowSize) + i) >= payloads.size()) {
						if (lastPacketBlockNumber == -1) {
							System.out.println("Last packet being sent.");
							lastWindow = true;
							lastPacketBlockNumber = (i - 1);
						}
						break;
					}

					// create a new data packet
					DataPacket dPack = new DataPacket(clientAddress, clientPort, DataPacket.TYPE,
						i, payloads.get((packetIndex * windowSize) + i));

					try { // try to send data packet
						System.out.println("Sending packet.");
						tInit.send(dPack, useDrop);
						System.out.println("Packet sent.");
					} catch (IOException io) {
						io.printStackTrace();
					}
				}


				// need to keep track of last retrieved block number
				if ((packetIndex * windowSize) >= payloads.size()) {
					if (lastPacketBlockNumber == -1) {
						lastWindow = true;
						lastPacketBlockNumber = windowSize - 1;
					}
				}

				try { // try to receive an ack from client
					System.out.println("Waiting for ack.");
					TftpPacket ackPack = (AckPacket) tInit.receive(3000);
					System.out.println("Ack received.");

					retries = 0;

					if (ackPack.getType() == AckPacket.TYPE) {
						AckPacket ack = (AckPacket) ackPack;
						int blkNum = ack.getBlockNumber();

						if ((blkNum == (windowSize - 1)) || (lastWindow && (lastPacketBlockNumber == blkNum))) {
							packetIndex++;
							if (lastWindow && (lastPacketBlockNumber == blkNum)) {
								break;
							}
						}
					}
				} catch (SocketTimeoutException e) {
					System.out.println("Socket timed out.");					
					retries++;
					if (retries >= 6) {
						System.out.println("Too many retries.");
						break;
					}
				} catch (IOException io) {
					io.printStackTrace();
				}
			}
		} else { // sequential operations
			int retries = 0;

			//System.out.println("Payload: " + payloads.size() + " packets");
			for (int i = 0; i < payloads.size();) {
				
				DataPacket dPack = new DataPacket(clientAddress, clientPort, DataPacket.TYPE, i, payloads.get(i));

				try {
					System.out.println("Sending packet at block number: " + i);
					tInit.send(dPack, this.useDrop);
				} catch (IOException io) {
					io.printStackTrace();
				}

				TftpPacket ackPack = null;

				try {
					ackPack = (AckPacket) tInit.receive(3000);
				} catch (SocketTimeoutException e) {
					System.out.println("Socket timed out.");
					retries++;
					if (retries >= 10) {
						System.out.println("Client not responding.");
						break;
					}
				} catch (IOException io) {
					io.printStackTrace();
				}

				if (ackPack != null) {
					if (ackPack.getType() == AckPacket.TYPE) {
						AckPacket ack = (AckPacket) ackPack;
						//System.out.println("Ack received with block number: " + ack.getBlockNumber());
						i = ack.getBlockNumber() + 1;
						retries = 0;
					} else {
						System.out.println("Did not receive correct ack packet.");
						retries++;
						if (retries >= 10) {
							break;
						}
					}
				}
			}
		}

		System.out.println("Finished sending data.");
		tInit.close();
		System.exit(1);
	}
}