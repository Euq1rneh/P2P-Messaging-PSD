package common;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public class Packet {

    private final String sender;
    private final String data;
    private String timestamp;

    private PacketType packetType;

    public Packet(String sender, String data, PacketType packetType){
        this.sender = sender;
        this.data = data;
        this.packetType = packetType;

        set_timestamp();
    }
    
    public Packet(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int senderLength = buffer.getInt();
        byte[] senderBytes = new byte[senderLength];
        buffer.get(senderBytes);
        this.sender = new String(senderBytes, StandardCharsets.UTF_8);

        int dataLength = buffer.getInt();
        byte[] dataBytes = new byte[dataLength];
        buffer.get(dataBytes);
        this.data = new String(dataBytes, StandardCharsets.UTF_8);

        int timestampLength = buffer.getInt();
        byte[] timestampBytes = new byte[timestampLength];
        buffer.get(timestampBytes);
        this.timestamp = new String(timestampBytes, StandardCharsets.UTF_8);

        int packetTypeLength = buffer.getInt();
        byte[] packetTypeBytes = new byte[packetTypeLength];
        buffer.get(packetTypeBytes);
        this.packetType = PacketType.valueOf(new String(packetTypeBytes, StandardCharsets.UTF_8));
    }

    public void set_timestamp(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        timestamp = now.format(formatter);
    }

    public String get_sender(){
        return sender;
    }

    public String get_data(){
        return data;
    }

    public PacketType get_packet_type(){
        return packetType;
    }
    
    public byte[] getBytes() {
    	byte[] senderBytes = sender.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] packetTypeBytes = packetType.toString().getBytes(StandardCharsets.UTF_8);

        int totalSize = 4 + senderBytes.length + 4 + dataBytes.length + 4 + timestampBytes.length + 4 + packetTypeBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(senderBytes.length);
        buffer.put(senderBytes);

        buffer.putInt(dataBytes.length);
        buffer.put(dataBytes);

        buffer.putInt(timestampBytes.length);
        buffer.put(timestampBytes);

        buffer.putInt(packetTypeBytes.length);
        buffer.put(packetTypeBytes);

        return buffer.array();
    }
    
	@Override
	public int hashCode() {
		return Objects.hash(data, packetType, sender, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Packet other = (Packet) obj;
		return Objects.equals(data, other.data) && packetType == other.packetType
				&& Objects.equals(sender, other.sender) && Objects.equals(timestamp, other.timestamp);
	}
    
    
}
