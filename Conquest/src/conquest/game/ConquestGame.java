package conquest.game;

import java.util.*;

import conquest.game.move.*;
import conquest.game.world.*;
import conquest.view.GUI;

public class ConquestGame implements Cloneable {
    public GameConfig config;
    GameMap map;
    PlayerInfo[] players;
    int round;
    int turn;
    public ArrayList<Region> pickableRegions;
    public Random random;
    GUI gui;
    
    public static final int nrOfStartingRegions = 3;
    
    // Create a game that is already in progress.
    public ConquestGame(GameConfig config, GameMap map, PlayerInfo[] players,
                        int round, int turn, ArrayList<Region> pickableRegions) {
        this.config = config;
        this.map = map;
        this.players = players != null ? players :
            new PlayerInfo[] { new PlayerInfo("1", "Player 1"), new PlayerInfo("2", "Player 2") };
        this.round = round;
        this.turn = turn;
        this.pickableRegions = pickableRegions;
        
        if (config.seed < 0) {
            config.seed = new Random().nextInt();
        }
        while (config.seed < 0)
            config.seed += Integer.MAX_VALUE;
        this.random = new Random(config.seed);
                
        recalculateStartingArmies();
    }
    
    // Create a new game with the given configuration.
    public ConquestGame(GameConfig config, PlayerInfo[] players) {
        this(config, makeInitMap(), players, 1, 1, null);
        initStartingRegions();
    }
    
    // Create a new game with default parameters.
    public ConquestGame() {
        this(new GameConfig(), null);
    }
    
    public void setGUI(GUI gui) {
        this.gui = gui;
    }
    
    @Override
    public ConquestGame clone() {
        // Unfortunately java.util.Random is not cloneable.  So a cloned game will have its
        // own random number generator, and actions applied to it may have different results
        // than in the original game.
        
        return new ConquestGame(
            config, map.clone(), new PlayerInfo[] { players[0].clone(), players[1].clone() },
            round, turn, new ArrayList<Region>(pickableRegions));
    }
    
    public GameMap getMap() { return map; }

    public int getRoundNumber() {
        return round;
    }
    
    public int getTurn() {
        return turn;
    }
    
    public PlayerInfo player(int i) {
        return players[i - 1];
    }
    
    public String playerName(int i) {
        return player(i).getName();
    }
    
    public PlayerInfo getPlayer(String playerId)
    {
        for (PlayerInfo p : players)
            if (p.getId().equals(playerId))
                return p;
        return null;
    }
    
    public PlayerInfo winningPlayer()
    {
        for (int i = 1 ; i <= 2 ; ++i)
            if (map.ownedRegionsByPlayer(player(i)).isEmpty())
            return player(3 - i);
        return null;
    }
    
    public boolean isDone() {
        return winningPlayer() != null || round > config.maxGameRounds;
    }
    
    //calculate how many armies each player is able to place on the map for the next round
    public void recalculateStartingArmies()
    {
        for (int p = 0 ; p < 2 ; ++p) {
            PlayerInfo pi = players[p];
            pi.setArmiesPerTurn(p == 0 && round <= 1 ? config.startingArmies / 2 :
                                                       config.startingArmies);
        }
        
        for(ContinentData superRegion : map.getContinents())
        {
            PlayerInfo player = getPlayer(superRegion.ownedByPlayer());
            if(player != null)
                player.setArmiesPerTurn(player.getArmiesPerTurn() + superRegion.getArmiesReward());
        }
    }
    
    static GameMap makeInitMap()
    {
        GameMap map = new GameMap();
        
        Map<Continent, ContinentData> continents = new TreeMap<Continent, ContinentData>(new Comparator<Continent>() {
            @Override
            public int compare(Continent o1, Continent o2) {
                return o1.id - o2.id;
            }           
        });
        
        for (Continent continent : Continent.values()) {
            ContinentData continentData = new ContinentData(continent, continent.id, continent.reward);
            continents.put(continent, continentData);
        }
        
        Map<Region, RegionData> regions = new TreeMap<Region, RegionData>(new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return o1.id - o2.id;
            }
        });
        
        for (Region region : Region.values()) {
            RegionData regionData = new RegionData(region, region.id, continents.get(region.continent));
            regions.put(region, regionData);
        }
        
        for (Region regionName : Region.values()) {
            RegionData region = regions.get(regionName);
            for (Region neighbour : regionName.getForwardNeighbours()) {
                region.addNeighbor(regions.get(neighbour));
            }
        }
        
        for (RegionData region : regions.values()) {
            map.add(region);
        }
        
        for (ContinentData continent : continents.values()) {
            map.add(continent);
        }

        // Make every region neutral with 2 armies to start with
        for(RegionData region : map.regions)
        {
            region.setPlayerName(Player.Neutral);
            region.setArmies(2);
        }

        return map;
    }
    
    void initStartingRegions() {
        pickableRegions = new ArrayList<Region>();
        int regionsAdded = 0;
        
        //pick semi random regions to start with
        for(Continent continent : Continent.values())
        {
            int nrOfRegions = continent.getRegions().size();
            while(regionsAdded < 2)
            {
                //get one random subregion from continent
                int randomRegionId = random.nextInt(nrOfRegions);
                
                Region randomRegion = continent.getRegions().get(randomRegionId);
                if(!pickableRegions.contains(randomRegion))
                {
                    pickableRegions.add(randomRegion);
                    regionsAdded++;
                }
            }
            regionsAdded = 0;
        }
    }
    
    public void chooseRegion(Region region) {
        if (!pickableRegions.contains(region))
            throw new Error("starting region is not pickable");
        
        map.getRegionData(region).setPlayerName(player(turn).getId());
        pickableRegions.remove(region);
        turn = 3 - turn;
    }
    
    public void placeArmies(List<PlaceArmiesMove> moves, List<Move> opponentMoves)
    {   
        String id = player(turn).getId();
        int left = player(turn).getArmiesPerTurn(); 
                
        for(PlaceArmiesMove move : moves)
        {
            RegionData region = map.getRegionData(move.getRegion());
            int armies = move.getArmies();
            
            if (!move.getPlayerName().equals(id))
                move.setIllegalMove("move by wrong player");
            else if (!region.ownedByPlayer(id))
                move.setIllegalMove(region.getId() + " not owned");
            else if (armies < 1)
                move.setIllegalMove("cannot place less than 1 army");
            else if (left <= 0)
                move.setIllegalMove("no armies left to place");
            else {
                if(armies > left) //player wants to place more armies than he has left
                    move.setArmies(left); //place all armies he has left
                
                left -= armies;
                region.setArmies(region.getArmies() + armies);

                if (region.isVisible(player(3 - turn)))
                    opponentMoves.add(move);
            }
        }
    }
    
    public static enum FightSide {
        ATTACKER,
        DEFENDER
    }
    
    public static class FightResult {
        public FightSide winner;
        
        public int attackersDestroyed;
        public int defendersDestroyed;
        
        public FightResult() {
            winner = null;
            attackersDestroyed = 0;         
            defendersDestroyed = 0;
        }
        
        public FightResult(FightSide winner, int attackersDestroyed, int defendersDestroyed) {
            this.winner = winner;
            this.attackersDestroyed = attackersDestroyed;
            this.defendersDestroyed = defendersDestroyed;
        }
        
        protected void postProcessFightResult(int attackingArmies, int defendingArmies) {      
            if(attackersDestroyed >= attackingArmies)
            {
                if (defendersDestroyed >= defendingArmies)
                    defendersDestroyed = defendingArmies - 1;
                
                attackersDestroyed = attackingArmies;
            }   
            
            if (defendersDestroyed >= defendingArmies) { //attack success
                winner = FightSide.ATTACKER;
            } else {
                winner = FightSide.DEFENDER;
            }
        }
    }

    static FightResult doOriginalAttack(Random random, int attackingArmies, int defendingArmies,
                                        double defenderDestroyedChance, double attackerDestroyedChance) {
        FightResult result = new FightResult();
        
        for(int t=1; t<=attackingArmies; t++) //calculate how much defending armies are destroyed
        {
            double rand = random.nextDouble();
            if(rand < defenderDestroyedChance) //60% chance to destroy one defending army
                result.defendersDestroyed++;
        }
        for(int t=1; t<=defendingArmies; t++) //calculate how much attacking armies are destroyed
        {
            double rand = random.nextDouble();
            if(rand < attackerDestroyedChance) //70% chance to destroy one attacking army
                result.attackersDestroyed++;
        }
        result.postProcessFightResult(attackingArmies, defendingArmies);
        return result;
    }
    
    static FightResult doContinualAttack(Random random,
            int attackingArmies, int defendingArmies,
            double defenderDestroyedChance, double attackerDestroyedChance) {
        
        FightResult result = new FightResult();
        
        while (result.attackersDestroyed < attackingArmies && result.defendersDestroyed < defendingArmies) {
            // ATTACKERS STRIKE
            double rand = random.nextDouble();
            if (rand < defenderDestroyedChance) ++result.defendersDestroyed;
            
            // DEFENDERS STRIKE
            rand = random.nextDouble();
            if (rand < attackerDestroyedChance) ++result.attackersDestroyed;
        }
        
        result.postProcessFightResult(attackingArmies, defendingArmies);
        return result;
    }

    static FightResult doAttack_ORIGINAL_A60_D70(
            Random random, int attackingArmies, int defendingArmies) {
        
        return doOriginalAttack(random, attackingArmies, defendingArmies, 0.6, 0.7);
    }
    
    static FightResult doAttack_CONTINUAL_1_1_A60_D70(
            Random random, int attackingArmies, int defendingArmies) {
        
        return doContinualAttack(random, attackingArmies, defendingArmies, 0.6, 0.7);
    }
    
    //see wiki.warlight.net/index.php/Combat_Basics
    private void doAttack(AttackTransferMove move)
    {
        RegionData fromRegion = map.getRegionData(move.getFromRegion());
        RegionData toRegion = map.getRegionData(move.getToRegion());
        int attackingArmies;
        int defendingArmies = toRegion.getArmies();
        
        if (fromRegion.getArmies() <= 1) {
            move.setIllegalMove(fromRegion.getId() + " attack " + "only has 1 army");
            return;
        }
        
        if(fromRegion.getArmies()-1 >= move.getArmies()) //are there enough armies on fromRegion?
            attackingArmies = move.getArmies();
        else
            attackingArmies = fromRegion.getArmies()-1;
        
        FightResult result = null;
        
        switch (config.fight) {
        case ORIGINAL_A60_D70:
            result = doAttack_ORIGINAL_A60_D70(random, attackingArmies, defendingArmies);
            break;
        case CONTINUAL_1_1_A60_D70:
            result = doAttack_CONTINUAL_1_1_A60_D70(random, attackingArmies, defendingArmies);
            break;
        }
        
        switch (result.winner) {
        case ATTACKER: //attack success
            fromRegion.setArmies(fromRegion.getArmies() - attackingArmies);
            toRegion.setPlayerName(move.getPlayerName());
            toRegion.setArmies(attackingArmies - result.attackersDestroyed);
            break; 
        case DEFENDER: //attack fail
            fromRegion.setArmies(fromRegion.getArmies() - result.attackersDestroyed);
            toRegion.setArmies(toRegion.getArmies() - result.defendersDestroyed);
            break;
        default:
            throw new RuntimeException("Unhandled FightResult.winner: " + result.winner);
        }
        
        if (gui != null) {
            gui.attackResult(fromRegion, toRegion, result.attackersDestroyed, result.defendersDestroyed);
        }
    }

    void validateAttackTransfers(List<AttackTransferMove> moves)
    {
        String id = player(turn).getId();
        int[] totalFrom = new int[Region.LAST_ID + 1];
        
        for (int i = 0 ; i < moves.size() ; ++i) {
            AttackTransferMove move = moves.get(i);
            RegionData fromRegion = map.getRegionData(move.getFromRegion());
            RegionData toRegion = map.getRegionData(move.getToRegion());

            if (!move.getPlayerName().equals(id))
                move.setIllegalMove("move by wrong player");
            else if (!fromRegion.ownedByPlayer(id))
                move.setIllegalMove(fromRegion.getId() + " attack/transfer not owned");
            else if (!fromRegion.isNeighbor(toRegion))
                move.setIllegalMove(toRegion.getId() + " attack/transfer not a neighbor");
            else if (move.getArmies() < 1)
                move.setIllegalMove("attack/transfer cannot use less than 1 army");
            else if (totalFrom[fromRegion.getId()] + move.getArmies() >= fromRegion.getArmies())
                move.setIllegalMove(fromRegion.getId() +
                        " attack/transfer has used all available armies");
            else {
                for (int j = 0 ; j < i ; ++j) {
                    AttackTransferMove n = moves.get(j);
                    if (n.getFromRegion() == move.getFromRegion() && n.getToRegion() == move.getToRegion()) {
                        move.setIllegalMove(fromRegion.getId() +
                                " attack/transfer has already attacked/transfered to this region");
                        break;
                    }
                }
                totalFrom[fromRegion.getId()] += move.getArmies();
            }
        }
    }
    
    public void attackTransfer(List<AttackTransferMove> moves, List<Move> opponentMoves) {
        validateAttackTransfers(moves);
        
        for (AttackTransferMove move : moves) {
            if(!move.getIllegalMove().equals("")) //the move is illegal
                continue;
            
            RegionData fromRegion = map.getRegionData(move.getFromRegion());
            RegionData toRegion = map.getRegionData(move.getToRegion());
            PlayerInfo player = player(turn);
            
            move.setArmies(Math.min(move.getArmies(), fromRegion.getArmies() - 1));

            PlayerInfo other = player(3 - turn);
            if (fromRegion.isVisible(other) || toRegion.isVisible(other))
                opponentMoves.add(move);
           
            if(toRegion.ownedByPlayer(player.getId())) //transfer
            {
                if (gui != null) {
                    gui.transfer(move);
                }
                fromRegion.setArmies(fromRegion.getArmies() - move.getArmies());
                toRegion.setArmies(toRegion.getArmies() + move.getArmies());
            }
            else //attack
            {
                if (gui != null) {
                    gui.attack(move);
                }
                doAttack(move);
            }
        }
        
        turn = 3 - turn;
        if (turn == 1)
            round++;
        
        recalculateStartingArmies();
    }
}
