import java.io.*;
import java.net.*;

public class ProxyServer {
	private static final int PORT = 2695;
	
	private static String usage;
	private static String host;
	private static boolean useSliding;
	private static boolean useDrop;

	public static void main (String args[]) throws IOException {
		usage = "usage: java java ProxyServer 4|6 <local address> [s] [d]";
		useSliding = false;
		useDrop = false;

		int version = 0;

		if (args.length < 2) {
			System.out.println(usage);
			return;
		} else if (args.length == 2) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);
		} else if (args.length == 3) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);		
			setConditions(args[2]);
		} else if (args.length == 4) {
			version = Integer.parseInt(args[0]);
			setIPVersion(version, args[1]);
			setConditions(args[2]);
			setConditions(args[3]);
		}

		// initialize page reader
		PageReader pageReader = new PageReader(useSliding, useDrop);
		pageReader.start();

		// initialize TftpInit
		TftpInit tftpServer = null;

		try { // try to create a connection on PORT and given host
			tftpServer = new TftpInit(PORT, host);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		if (tftpServer != null) { // if connection made wait for a request
			for (;;) {
				TftpPacket tPack;
				try {
					// try to receive a read request packet
					System.out.println("Waiting for packet.");
					tPack = tftpServer.receive(0);
					System.out.println("Received packet.");

					/*
					* Check if received packet is a read request
					* If it is place it in the queue so it can be read
					*/
					if (tPack.getType() == RrqPacket.TYPE) {
						pageReader.addToQueue((RrqPacket) tPack);
					}
				} catch (IOException io) {
					io.printStackTrace();
				}
			}
		} else {
			System.out.println("TFTP not initalized correctly.");
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

	// set program conditions s = sliding window, d = packet drops
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