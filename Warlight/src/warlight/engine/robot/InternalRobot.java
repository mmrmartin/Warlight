package warlight.engine.robot;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.*;

import warlight.bot.*;
import warlight.engine.Robot;
import warlight.game.*;
import warlight.game.move.*;
import warlight.game.world.WorldRegion;

public class InternalRobot implements Robot {
    
    private class MyKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {}
        
        @Override
        public void keyReleased(KeyEvent e) {}
        
        @Override
        public void keyPressed(KeyEvent e) {
            if (InternalRobot.this.config.team == Team.PLAYER_1) {
                if (Character.toLowerCase(e.getKeyChar()) == 'h') {
                    hijacked = !hijacked;
                    if (config.gui != null) {
                        config.gui.showNotification(
                            hijacked ? InternalRobot.this.config.player + " hijacked!" : InternalRobot.this.config.player + " resumed!"
                        );
                    }
                }
            }
            if (InternalRobot.this.config.team == Team.PLAYER_2) {
                if (Character.toLowerCase(e.getKeyChar()) == 'j') {
                    hijacked = !hijacked;
                    if (config.gui != null) {
                        config.gui.showNotification(
                            hijacked ? InternalRobot.this.config.player + " hijacked!" : InternalRobot.this.config.player + " resumed!"
                        );
                    }
                }
            }
        }
    }
    
    private Bot bot;

    private RobotConfig config;
    
    private boolean hijacked = false;
    
    private HumanRobot humanHijack;

    private MyKeyListener myKeyListener;

    private String botFQCN;
    
    public InternalRobot(int player, BotLoader botLoader, String botFQCN) throws IOException {
        this.botFQCN = botFQCN;
        
        bot = BotParser.constructBot(botLoader, botFQCN);
        System.out.println(player + " -> " + botFQCN);
        
        humanHijack = new HumanRobot();
    }
    
    @Override
    public void setup(RobotConfig config) {
        this.config = config;
        
        humanHijack.setup(config);
        
        if (config.gui != null) {
            myKeyListener = new MyKeyListener();
            config.gui.addKeyListener(myKeyListener);
        }

        bot.init(config.timeoutMillis);
    }
    
    @Override
    public WorldRegion getStartingRegion(GameState state)
    {
        if (hijacked) {
            return humanHijack.getStartingRegion(state);            
        }
        return bot.chooseRegion(state);
    }
    
    @Override
    public List<PlaceArmiesMove> getPlaceArmiesMoves(GameState state)
    {
        if (hijacked) {
            return humanHijack.getPlaceArmiesMoves(state);        
        }
        return bot.placeArmies(state);
    }
    
    @Override
    public List<AttackTransferMove> getAttackTransferMoves(GameState state)
    {
        if (hijacked) {
            return humanHijack.getAttackTransferMoves(state);    
        }
        return bot.moveArmies(state);
    }
    
    @Override
    public void writeInfo(String info){
        humanHijack.writeInfo(info);
    }

    public boolean isRunning() {
        return bot != null;
    }
    
    public void finish() {
        if (config.gui != null) {
            config.gui.removeKeyListener(myKeyListener);
        }
        bot = null;
        if (humanHijack != null) {
            try {
                humanHijack.finish();
            } catch (Exception e) {                
            }
            humanHijack = null;
        }
    }

    @Override
    public int getRobotPlayer() {
        if (config == null) return 0;
        return config.player;
    }
    
    public String getRobotPlayerName() {
        if (config == null) return botFQCN.substring(1+botFQCN.lastIndexOf("."));
        return botFQCN.substring(1+botFQCN.lastIndexOf("."));
    }

}
