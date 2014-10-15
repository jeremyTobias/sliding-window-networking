import java.net.DatagramPacket;
import java.net.InetAddress;

public class RrqPacket extends TftpPacket {
	public static final int TYPE = 1; // TFTP packet type
	private String url; // url to be read from

	// read mode, only using 'octet' transmission
	private byte[] modeArray = {(byte)'o', (byte)'c', (byte)'t', (byte)'e', (byte)'t',};

	public RrqPacket(InetAddress addr, int port, int type, String url) {
		super(addr, port, type);
		this.url = url;
	}

	public RrqPacket(DatagramPacket dPack, int type) {
		super(dPack, type);

		byte[] pkt_data = dPack.getData();

		/*
		* Parse the packet to get the URL
		*/
		StringBuffer strBuffer = new StringBuffer();

		for (int i = 2; i < dPack.getLength(); i++) {
			if (pkt_data[i] == 0) break;
			strBuffer.append((char)pkt_data[i]);
		}

		url = strBuffer.toString();
	}

	// generate an RRQ packet
	@Override
	public DatagramPacket getDatagramPacket() {
		byte[] pkt_data = new byte[this.url.length() + this.modeArray.length + 4];
		
		pkt_data[0] = 0;
		pkt_data[1] = (byte)this.getType();

		System.arraycopy(this.url.getBytes(), 0, pkt_data, 2, this.url.length());

		pkt_data[this.url.length() + 2] = 0;

		System.arraycopy(this.modeArray, 0, pkt_data, this.url.length() + 3, this.modeArray.length);

		pkt_data[this.modeArray.length + this.url.length() + 3] = 0;

		return new DatagramPacket(pkt_data, pkt_data.length, this.getAddress(), this.getPort());
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}
}