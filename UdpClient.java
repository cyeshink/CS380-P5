import java.io.*;
import java.net.*;
import java.util.*;

public class UdpClient {

	public static void main(String[] args) throws Exception {
		try (Socket socket = new Socket("www.codebank.xyz", 38005)) {
			InputStream is = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			double avgRTT = 0;

			// Create a handshake
			byte[] hsData = { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
			byte hsPacket[] = createIPv4(hsData);
			out.write(hsPacket);

			// Get and print Response

			System.out.print("Handshake response: 0x" + Integer.toHexString(is.read()).toUpperCase());
			for(int i = 0;i<3;i++){
				System.out.print(Integer.toHexString(is.read()).toUpperCase());
			}

			// Get and print Port
			int port = 0;
			port = (is.read() << 8);
			port += is.read();
			System.out.println("\nPort number received: " + port + "\n");

			int data = 2;
			// Send packets and get response/time
			for (int i = 0; i < 12; i++) {
				byte[] udp = createUDP(port, data);
				byte[] ipv4 = createIPv4(udp);
				
				long receive = 0;
				long rtt = 0;
				long send = System.currentTimeMillis();

				out.write(ipv4);
				System.out.println("Sending packet with " + data + " bytes of data");
				System.out.print("Response: 0x" + Integer.toHexString(is.read()).toUpperCase());
				for(int j = 0;j<3;j++){
					System.out.print(Integer.toHexString(is.read()).toUpperCase());
				}
				receive = System.currentTimeMillis();
				rtt = receive - send;
				avgRTT += rtt;
				System.out.println("\nRTT: " + rtt + "ms\n");
				data *= 2;

			}

			avgRTT /= 12;
			System.out.printf("Average RTT: %.2fms\n", avgRTT);
		} catch (Exception e) {

		}
	}

	private static byte[] createIPv4(byte[] data) {

		byte[] packet = new byte[20 + data.length];
		short size = (short) (20 + data.length);

		packet[0] = 0x45; //Version and Header Length
		packet[1] = 0; //Type of Service
		packet[2] = (byte) ((size >> 8) & 0xff); //Length
		packet[3] = (byte) (size & 0xff); //Length
		packet[4] = 0; //Identification
		packet[5] = 0; //Identification
		packet[6] = 0x40; //Flags
		packet[7] = 0; //Offset
		packet[8] = 50; //Time To Live
		packet[9] = 17; //Protocol
		packet[12] = (byte) 92; //Source Address
		packet[13] = (byte) 93; //Source Address
		packet[14] = (byte) 123; //Source Address
		packet[15] = (byte) 232; //Source Address
		packet[16] = (byte) 52; //Destination Address
		packet[17] = (byte) 37; //Destination Address
		packet[18] = (byte) 88; //Destination Address
		packet[19] = (byte) 154; //Destination Address
		short check = checksum(packet);
		packet[10] = (byte) (check >>> 8); //Check Sum
		packet[11] = (byte) check; //Check Sum
		for (int i = 0; i < data.length; i++) {
			packet[20 + i] = (byte) data[i];
		}

		return packet;

	}

	private static byte[] createUDP(int destPort, int size) {
		byte[] packet = new byte[8 + size];
		packet[0] = 0; // Source Port
		packet[1] = 0; // Source Port
		packet[2] = (byte) (destPort >> 8); // Destination Port
		packet[3] = (byte) destPort; // Destination Port
		packet[4] = (byte) (size >> 8); // Length
		packet[5] = (byte) size; // Length
		Random rand = new Random();
		for (int i = 0; i < size; i++) {
			packet[i + 8] = (byte) rand.nextInt();
		}
		byte[] pseudoHeader = new byte[12];
		pseudoHeader[0] = (byte) 92; // Source Address
		pseudoHeader[1] = (byte) 93; // Source Address
		pseudoHeader[2] = (byte) 123; // Source Address
		pseudoHeader[3] = (byte) 232; // Source Address
		pseudoHeader[4] = (byte) 52; // Destination Address
		pseudoHeader[5] = (byte) 37; // Destination Address
		pseudoHeader[6] = (byte) 88; // Destination Address
		pseudoHeader[7] = (byte) 154; // Destination Address
		pseudoHeader[8] = 0; // Reserved
		pseudoHeader[9] = 17; // Protocol
		pseudoHeader[10] = packet[4];
		pseudoHeader[11] = packet[5];

		// Checksum
		byte[] checkUdp = new byte[8 + size + 12];
		for (int i = 0; i < checkUdp.length; i++) {
			if (i < (8 + size)) {
				checkUdp[i] = packet[i];
			} else {
				checkUdp[i] = pseudoHeader[i - (size + 8)];
			}
		}

		short checksum = checksum(checkUdp);
		packet[6] = (byte) ((checksum >> 8) & 0xff);
		packet[7] = (byte) (checksum & 0xff);

		return packet;
	}

	public static short checksum(byte[] b) {
        int l = b.length;
        int i = 0;
        long total = 0;
        long sum = 0;

        while (l > 1) {
            sum = sum + ((b[i] << 8 & 0xFF00) | ((b[i + 1]) & 0x00FF));
            i = i + 2;
            l = l - 2;
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum++;
            }
        }
        if (l > 0) {
            sum += b[i] << 8 & 0xFF00;
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum++;
            }
        }
        total = (~((sum & 0xFFFF) + (sum >> 16))) & 0xFFFF;
        return (short) total;
    }

}