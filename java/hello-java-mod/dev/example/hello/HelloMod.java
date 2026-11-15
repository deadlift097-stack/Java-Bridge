package dev.example.hello;

import com.modhost.api.EventHandler;
import com.modhost.api.Events;
import com.modhost.api.ModContext;
import com.modhost.api.ModHostMod;

public class HelloMod implements ModHostMod {
    private ModContext ctx;
    private int ticks;

    public void onLoad(ModContext ctx) {
        this.ctx = ctx;
        ctx.logger.info("Hello from Java! data dir: " + ctx.dataDir);

        // settings: shown under "Settings" next to this mod in the in-game menu, saved to config.properties
        ctx.config.toggle("logTicks", "Log tick counter", true)
                  .number("logEverySeconds", "Log every (seconds)", 60, 5, 300)
                  .text("greeting", "Greeting text", "Hello!");

        // register handlers in onLoad; they are skipped automatically while the mod is disabled
        Events.onTick(new Runnable() {
            public void run() {
                if (!HelloMod.this.ctx.config.getBoolean("logTicks")) return;
                if (++ticks >= HelloMod.this.ctx.config.getDouble("logEverySeconds") * 20) {
                    ticks = 0;
                    HelloMod.this.ctx.logger.info(HelloMod.this.ctx.config.getString("greeting") + " (tick log)");
                }
            }
        });
        Events.on("app.resumed", new EventHandler() {
            public void handle(String name, String data) {
                HelloMod.this.ctx.logger.info("game screen resumed: " + data);
            }
        });
        Events.on("config.changed", new EventHandler() {
            public void handle(String name, String data) {
                HelloMod.this.ctx.logger.info("setting changed: " + data);
            }
        });
    }

    public void onEnable()  { ctx.logger.info("enabled"); }
    public void onDisable() { ctx.logger.info("disabled"); }
}
