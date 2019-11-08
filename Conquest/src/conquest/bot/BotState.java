// Copyright 2014 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//    
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package conquest.bot;

import java.util.ArrayList;

import conquest.game.GameMap;
import conquest.game.Phase;
import conquest.game.RegionData;
import conquest.game.GameState;
import conquest.game.ContinentData;
import conquest.game.world.Continent;
import conquest.game.world.Region;

public class BotState {
    private GameState state;

    public BotState()
    {
        state = new GameState(null, new GameMap(), null, new ArrayList<Region>());
    }
    
    public void updateSettings(String key, String value)
    {
        if (key.equals("your_player_number"))
            state.setTurn(Integer.parseInt(value));
    }
    
    public void nextRound() {
        state.setRoundNumber(state.getRoundNumber() + 1);
    }
    
    public void setPhase(Phase phase) {
        state.setPhase(phase);
    }
    
    //initial map is given to the bot with all the information except for player and armies info
    public void setupMap(String[] mapInput)
    {
        GameMap map = state.getMap();

        if(mapInput[1].equals("continents"))
        {
            for(int i=2; i<mapInput.length; i++)
            {
                try {
                    int continentId = Integer.parseInt(mapInput[i]);
                    i++;
                    int reward = Integer.parseInt(mapInput[i]);
                    map.add(new ContinentData(Continent.forId(continentId), continentId, reward));
                }
                catch(Exception e) {
                    System.err.println("Unable to parse Continents");
                }
            }
        }
        else if(mapInput[1].equals("regions"))
        {
            for(int i=2; i<mapInput.length; i++)
            {
                try {
                    int regionId = Integer.parseInt(mapInput[i]);
                    i++;
                    int continentId = Integer.parseInt(mapInput[i]);
                    ContinentData continent = map.getContinent(continentId);
                    map.add(new RegionData(Region.forId(regionId), regionId, continent));
                }
                catch(Exception e) {
                    System.err.println("Unable to parse Regions " + e.getMessage());
                }
            }
        }
        else if(mapInput[1].equals("neighbors"))
        {
            for(int i=2; i<mapInput.length; i++)
            {
                try {
                    RegionData region = map.getRegion(Integer.parseInt(mapInput[i]));
                    i++;
                    String[] neighborIds = mapInput[i].split(",");
                    for(int j=0; j<neighborIds.length; j++)
                    {
                        RegionData neighbor = map.getRegion(Integer.parseInt(neighborIds[j]));
                        region.addNeighbor(neighbor);
                    }
                }
                catch(Exception e) {
                    System.err.println("Unable to parse Neighbors " + e.getMessage());
                }
            }
        }
    }
    
    //regions from which a player is able to pick his preferred starting regions
    public void setPickableStartingRegions(String[] mapInput)
    {
        ArrayList<Region> regions = new ArrayList<Region>();
        
        for(int i=2; i<mapInput.length; i++)
        {
            int regionId;
            try {
                regionId = Integer.parseInt(mapInput[i]);
                Region pickableRegion = Region.forId(regionId);
                regions.add(pickableRegion);
            }
            catch(Exception e) {
                System.err.println("Unable to parse pickable regions " + e.getMessage());
            }
        }

        state.setPickableRegions(regions);
    }
    
    //visible regions are given to the bot with player and armies info
    public void updateMap(String[] mapInput)
    {
        for(int i=1; i<mapInput.length; i++)
        {
            try {
                RegionData region = state.getMap().getRegion(Integer.parseInt(mapInput[i]));
                int owner = Integer.parseInt(mapInput[i+1]);
                int armies = Integer.parseInt(mapInput[i+2]);
                
                region.setOwner(owner);
                region.setArmies(armies);
                i += 2;
            }
            catch(Exception e) {
                System.err.println("Unable to parse Map Update " + e.getMessage());
            }
        }
    }
    
    public int getRoundNumber(){
        return state.getRoundNumber();
    }
    
    public int getMyPlayerNumber() {
        return state.me();
    }
    
    /**
     * Map that is updated via observations.
     * @return
     */
    public GameMap getMap(){
        return state.getMap();
    }
    
    public ArrayList<Region> getPickableStartingRegions(){
        return state.getPickableRegions();
    }

    public GameState toConquestGame() {
        return state;
    }
    
}
