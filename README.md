# AutoRef
This repository contains the source code for the automatic referee for RoboCup SSL tournaments built for the RoboTeam Twente.

## Usage
This automatic referee is dependent on the world processing software from RoboTeam Twente. This software package is 
available as part of our AI docker image. This image can be pulled with:
```
docker pull roboteamtwente/roboteam:latest
```

Next one will have to create the actual docker container:
```
docker run -it --net=host --name roboteam roboteamtwente/roboteam
```

After creation the container may be started and the observer software can be executed:
```
docker start roboteam
docker exec roboteam roboteam_world/roboteam_observer
```

As the project is managed and built using Gradle, any way of using Gradle should work with this project, such as the Gradle integration
of your IDE. Explained below is the basic usage guidelines that will require no extra installation steps apart from Java itself.

Make sure you have some distribution of at least Java 17 installed on your machine. On Ubuntu, you can install OpenJDK 
JRE 17 using the following command:
```bash
sudo apt install openjdk-17-jre -y
```

Finally, you can use Gradle to run the application:
```
./gradlew run
```

### Program Arguments
The program accepts a few program arguments:
```
--world-ip=...         the IP on which the application tries to connect to the RoboTeam World Observer to [default = 127.0.0.1]
--world-port=...       the port on which the application tries to connect to the RoboTeam World Observer to [default = 5558]
--gc-ip=...            the IP on which the application tries to connect to the Game Controller [default = 127.0.0.1]
--gc-port=...          the port on which the application tries to connect to the Game Controller [default = 10007]
--active               if the application should start in active mode instead of passive [default = passive]
```

When running using gradle, these arguments can be specified in the following way:
```bash
./gradlew run --args="--active"
```

## Rules
At the moment this automated referee does not implement all the rules that the average automated referee is supposed to
validate, in the table below one finds all the rules that are actually being kept track of right now.

Each rule is implemented, however our auto referee cannot reliably make decisions in the Z-plane (height), hence these
can be unreliable. Additionally, sometimes objects move too fast, causing us to miss certain fouls.

| Rule                                  | Implemented | Implementation                                                                                                                                                                                                   | Notes                                                                          |
|---------------------------------------|:-----------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| Attacker double touched ball          |     [x]     | [AttackerDoubleTouchedBallValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/AttackerDoubleTouchedBallValidator.java)               | Failed the unittest when chipping                                              |
| Ball left field touch line            |     [x]     | [BallLeftFieldTouchLineValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BallLeftFieldTouchLineValidator.java)                     | Fails if the ball leaves mid-air, which is not supposed to happen              |
| Ball left field goal line             |     [x]     | [BallLeftFieldGoalLineValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BallLeftFieldGoalLineValidator.java)                       |                                                                                |
| Aimless kick                          |     [x]     | [AimlessKickValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/AimlessKickValidator.java)                                           | Only applied in division B                                                     |
| Possible goal                         |     [x]     | [PossibleGoalValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/PossibleGoalValidator.java)                                         | Goals are detected, but we cannot check whether the ball was kicked or chipped |
| Defender in defense area              |     [x]     | [DefenderInDefenseAreaValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/DefenderInDefenseAreaValidator.java)                       | Failed the unittest when chipping                                              |
| Attacker touched ball in defense area |     [x]     | [AttackerTouchedBallInDefenseAreaValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/AttackerTouchedBallInDefenseAreaValidator.java) | Failed the unittest when chipping                                              |
| Boundary crossing                     |     [x]     | [BoundaryCrossingValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BoundaryCrossingValidator.java)                                 |                                                                                |
| Bot kicked ball too fast              |     [x]     | [BotKickedBallTooFastValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotKickedBallTooFastValidator.java)                         |                                                                                |
| Bot dribbled ball too far             |     [x]     | [BotDribbledBallTooFarValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotDribbledBallTooFarValidator.java)                       |                                                                                |
| Penalty kick failed                   |     [x]     | [PenaltyKickFailedValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/PenaltyKickFailedValidator.java)                               |                                                                                |
| Bot crashing unique                   |     [x]     | [BotCrashingValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotCrashingValidator.java)                                           |                                                                                |
| Bot crashing drawn                    |     [x]     | [BotCrashingValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotCrashingValidator.java)                                           |                                                                                |
| Attacker too close to defense area    |     [x]     | [AttackerTooCloseToDefenseAreaValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/AttackerTooCloseToDefenseAreaValidator.java)       |                                                                                |
| Defender too close to kick point      |     [x]     | [DefenderTooCloseToKickPointValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/DefenderTooCloseToKickPointValidator.java)           |                                                                                |
| Bot too fast in stop                  |     [x]     | [BotTooFastInStopValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotTooFastInStopValidator.java)                                 |                                                                                |
| Bot interfered placement              |     [x]     | [BotInterferedPlacementValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/BotInterferedPlacementValidator.java)                     |                                                                                |
| Placement succeeded                   |     [x]     | [PlacementSucceededValidator.java](https://github.com/RoboTeamTwente/roboteam_autoref/blob/main/src/main/java/nl/roboteamtwente/autoref/validators/PlacementSucceededValidator.java)                             |                                                                                |

