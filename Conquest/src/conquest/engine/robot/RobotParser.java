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

package conquest.engine.robot;

import java.util.ArrayList;

import conquest.game.*;
import conquest.game.move.*;

public class RobotParser {
    
    public ArrayList<Move> parseMoves(GameState state, String input, int player)
    {
        ArrayList<Move> moves = new ArrayList<Move>();
        
        try {
            input = input.trim();
            if(input.length() <= 1)
                return moves;
            
            String[] split = input.split(",");
            
            for(int i=0; i<split.length; i++)
            {
                if(i > 50){
                    //player.getBot().addToDump("Maximum number of moves reached, max 50 moves are allowed");
                    break;
                }
                Move move = parseMove(state, split[i], player);
                if(move != null)
                    moves.add(move);
            }
        }
        catch(Exception e) {
            //player.getBot().addToDump("Move input is null");
        }
        return moves;
    }

    //returns the correct Move. Null if input is incorrect.
    private Move parseMove(GameState state, String input, int player)
    {
        int armies = -1;
        
        String[] split = input.trim().split(" ");

        if(split[0].equals("place_armies"))        
        {
            Region region = parseRegion(state, split[1], input);

            try { armies = Integer.parseInt(split[2]); }
            catch(Exception e) { errorOut("Number of armies input incorrect", input);}
        
            if(!(region == null || armies == -1))
                return new PlaceArmiesMove(region, armies);
            return null;
        }
        else if(split[0].equals("attack/transfer"))
        {
            Region fromRegion = parseRegion(state, split[1], input);
            Region toRegion = parseRegion(state, split[2], input);
            
            try { armies = Integer.parseInt(split[3]); }
            catch(Exception e) { errorOut("Number of armies input incorrect", input);}

            if(!(fromRegion == null || toRegion == null || armies == -1))
                return new AttackTransferMove(fromRegion, toRegion, armies);
            return null;
        }

        errorOut("Bot's move format incorrect", input);
        return null;
    }
    
    //parse the region given the id string.
    private Region parseRegion(GameState state, String regionId, String input)
    {
        int id = -1;
        
        try { id = Integer.parseInt(regionId); }
        catch(NumberFormatException e) { errorOut("Region id input incorrect", input); return null;}
        
        return state.getRegion(id);
    }
    
    public Region parseStartingRegion(GameState state, String input)
    {
        return parseRegion(state, input, input);
    }

    private void errorOut(String error, String input)
    {
        System.out.println("Parse error: " + error + " (" + input + ")");
    }

}
