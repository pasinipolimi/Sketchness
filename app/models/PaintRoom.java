package models;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PaintRoom {
	
    public String name;
    // The list of all connected painters (identified by ids)
    public Map<Integer, Painter> painters = new ConcurrentHashMap<Integer, models.Painter>();
    public AtomicInteger counter = new AtomicInteger(0);
    public AtomicInteger connections = new AtomicInteger(0);
    
    private boolean matchStarted = false;

	public PaintRoom(String name) {
		this.name = name;
	}

    public void createPainter(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
        counter.incrementAndGet();
        connections.incrementAndGet();
        final int pid = counter.intValue(); // the painter id

        // in: handle messages from the painter
        in.onMessage(new F.Callback<JsonNode>() {
            @Override
            public void invoke(JsonNode json) throws Throwable {
                String type = json.get("type").getTextValue();

                // The painter wants to change some of his property
                if("change".equals(type)) {
                    Painter painter = painters.get(pid);
                    if(painter == null) {
                        painter = new Painter(out);
                        painters.put(pid, painter);

                        // Inform the painter who he is (which pid, he can then identify himself)
                        ObjectNode self = Json.newObject();
                        self.put("type", "youAre");
                        self.put("pid", pid);
                        self.put("role", painter.role);
                        painter.channel.write(self);

                        // Inform the list of other painters
                        for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
                            ObjectNode other = (ObjectNode)entry.getValue().toJson();
                            other.put("pid", entry.getKey());
                            painter.channel.write(other);
                        }
                    }
                    painter.updateFromJson(json);
                }

                ObjectNode node = ((ObjectNode)json);
                node.put("pid", pid);
                PaintRoom.this.notifyAll(node);
            }
        });

        // User has disconnected.
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                painters.remove(pid);
                connections.decrementAndGet();

                ObjectNode json = Json.newObject();
                json.put("type", "disconnect");
                json.put("pid", pid);

                PaintRoom.this.notifyAll(json);

                Logger.debug("(pid:"+pid+") disconnected.");
                Logger.info(connections+" painter(s) currently connected.");
            }
        });

        Logger.debug("(pid:"+pid+") connected.");
        Logger.info(connections+" painter(s) currently connected.");
    }

    public void notifyAll(JsonNode json) {
        for(Painter painter : painters.values()) {
            painter.channel.write(json);
        }
    }
    
    public void matchStarted(String sketcher) 
    {
        matchStarted=true;
        for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
            if(entry.getValue().name.equals(sketcher))
            {
                ObjectNode self = Json.newObject();
                self.put("type", "role");
                self.put("role","SKETCHER");
                entry.getValue().channel.write(self);
            }
            else
            {
                ObjectNode self = Json.newObject();
                self.put("type", "role");
                self.put("role","GUESSER");
                entry.getValue().channel.write(self);
            }
        }
    }
	
}
