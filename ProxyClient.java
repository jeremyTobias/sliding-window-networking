import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;

public class ProxyClient {
	private static final int PORT = 2695;

	private static String usage;
	private static String host;
	private static boolean useSliding;
	private static boolean useDrop;

	public static void main(String[] args) throws IOException {
		usage = "usage: java ProxyClient 4|6 <remote address> <http://'some url'> [s] [d]";
		useSliding = false;
		useDrop = false;

		int version = 0;
		String url = "";

		if (args.length < 3) {
			System.out.println(usage);
			return;
		} else if (args.length == 3) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);
			url = args[2];
		} else if (args.length == 4) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);
			url = args[2];
			setConditions(args[3]);
		} else if (args.length == 5) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);
			url = args[2];
			setConditions(args[3]);
			setConditions(args[4]);
		}

		InetAddress proxyAddr = InetAddress.getByName(host);

		String pagename = "data";

		boolean finished = true;

		File save = new File(pagename);

		FileOutputStream out = new FileOutputStream(save);

		TftpInit tInit = new TftpInit();

		RrqPacket rrqPacket = new RrqPacket(proxyAddr, PORT, RrqPacket.TYPE, url);

		tInit.send(rrqPacket, useDrop);

		int lastPktSize = 0;
		int dataSize = 0;
		int numPkts = 0;

		long startTime = System.nanoTime();

		if (useSliding) {
			int windowSize = 4;
			ArrayList<DataPacket> window = new ArrayList<DataPacket>();

			int lastDataLength = 0;

			do {
				TftpPacket tPack = tInit.receive(0);

				if (tPack.getType() == DataPacket.TYPE) {
					DataPacket dPack = (DataPacket) tPack;

					int blkNum = dPack.getBlockNumber();

					lastDataLength = dPack.getPayloadLength();

					boolean added = false;

					for (int i = 0; i < window.size(); i++) {
						if (blkNum < window.get(i).getBlockNumber()) {
							window.add(i, dPack);
							added = true;
							break;
						} else if (blkNum == window.get(i).getBlockNumber()) {
							added = true;
							break;
						}
					}

					if (!added) {
						window.add(dPack);
					}

					if ((window.size() == windowSize) || (lastDataLength < DataPacket.MAX_DATA)) {
						if (blkNum == (window.size() - 1)) {
							for (int i = 0; i < window.size(); i++) {
								out.write(window.get(i).getPayload());
								numPkts++;
								dataSize += window.get(i).getPayloadLength() + 4;
							}

							AckPacket ackPack = new AckPacket(dPack.getAddress(), dPack.getPort(), AckPacket.TYPE, blkNum);

							tInit.send(ackPack, useDrop);

							if (lastDataLength < DataPacket.MAX_DATA) {
								finished = true;
							}

							window.clear();
						}
					}
				} else {
					System.out.println("Did not receive a data packet.");
				}
			} while (lastDataLength >= DataPacket.MAX_DATA); 
		} else {
			int nextBlkNum = 0;
			do {
				TftpPacket tPack = tInit.receive(0);

				if (tPack.getType() == DataPacket.TYPE) {
					DataPacket dPack = (DataPacket) tPack;

					if (nextBlkNum == dPack.getBlockNumber()) {
						lastPktSize = dPack.getPayloadLength();

						out.write(dPack.getPayload());
						numPkts++;
						nextBlkNum++;
						dataSize += lastPktSize + 4;
					}

					AckPacket ackPack = new AckPacket(dPack.getAddress(), dPack.getPort(), AckPacket.TYPE, dPack.getBlockNumber());
					tInit.send(ackPack, useDrop);
				} else {
					System.out.println("Did not receive a data packet.");
					finished = false;
					break;
				}
			} while (lastPktSize >= DataPacket.MAX_DATA);
		}

		if (finished) {
			out.flush();

			long endTime = System.nanoTime();
			long totalTime = endTime - startTime;

			double totalTimeSecs = (double)(totalTime / 1000000000.0);
			double dataInKb = (double)(dataSize / (2 << 6));
			double throughput = dataInKb / totalTimeSecs;

			genDataFile(version, throughput);

			System.out.println("Finished receiving data.");
			System.out.println("Total data received: " + dataSize);
			System.out.println("Total time to receive: " + totalTimeSecs + " secs");
			System.out.println("Throughput: " + throughput + " Kb/secs");
			System.out.println("Number of packets received: " + numPkts);
		} else {
			System.out.println("Did not finish.");
		}
	}

	private static void genDataFile(int ipVersion, double throughput) throws IOException {
		int fileVersion = (int)(System.nanoTime() / 10000);

		try {
			FileWriter writer = new FileWriter("proxy_data_" + fileVersion + ".csv");

			// write the column headers
			writer.append("IP Version");
			writer.append(',');
			writer.append("Drop Simulation");
			writer.append(',');
			writer.append("Sliding Window");
			writer.append(',');
			writer.append("Throughput");
			writer.append('\n');

			// write data
			writer.append(String.valueOf(ipVersion));
			writer.append(',');
			writer.append(String.valueOf(useDrop));
			writer.append(',');
			writer.append(String.valueOf(useSliding));
			writer.append(',');
			writer.append(String.valueOf(throughput));

			// write file
			writer.flush();
			writer.close();
		} catch (IOException io) {
			System.err.println("Could not write file.");
		}
	}

	// set IPv preferrence
	private static void setIPVersion(int version, String h) {
		//try {
			//InetAddress [] inet = InetAddress.getAllByName(h);
			host = h;
			if (version == 4) {
				System.setProperty("java.net.preferIPv4Stack", "true");
				/*for (InetAddress addr : inet) {
					if (addr instanceof Inet4Address) {
						host = addr.getHostAddress();
					}
				}*/
			} else if (version == 6) {
				System.setProperty("java.net.preferIPv6Addresses", "true");
				System.setProperty("java.net.preferIPv4Stack", "false");
				/*NetworkInterface nif = null;
				for (InetAddress addr : inet) {
					if (addr instanceof Inet4Address) {
						Inet6Address inet6addr = Inet6Address.getByAddress(h, addr.getAddress(), nif);
						host = inet6addr.getHostAddress();
						
					}
				}*/
			} else {
				System.out.println(usage);
				return;
			} 
		/*} catch (UnknownHostException uhe) { 
				System.out.println("Unknown host.");
					System.out.println(usage);
					return;
		}

		System.out.println("IPv" + version + " address: " + host);*/
	}

	private static void setConditions(String cond) {
		if (cond.equals("s")) {
			useSliding = true;
		} else if (cond.equals("d")) {
			useDrop = true;
		} else {
			System.out.println(usage);
			return;
		}
	}
}