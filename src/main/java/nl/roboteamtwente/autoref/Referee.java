package nl.roboteamtwente.autoref;

import nl.roboteamtwente.autoref.model.Game;
import nl.roboteamtwente.autoref.validators.AttackerTouchedBallInDefenseAreaValidator;
import nl.roboteamtwente.autoref.validators.BallLeftFieldGoalLineValidator;
import nl.roboteamtwente.autoref.validators.BallLeftFieldTouchLineValidator;
import nl.roboteamtwente.autoref.validators.DefenderInDefenseAreaValidator;

import java.util.ArrayList;
import java.util.List;

public class Referee {
    private static final List<RuleValidator> RULE_VALIDATORS = List.of(
            new AttackerTouchedBallInDefenseAreaValidator(),
            new BallLeftFieldTouchLineValidator(),
            new BallLeftFieldGoalLineValidator(),
            new DefenderInDefenseAreaValidator()
    );

    private List<RuleValidator> activeValidators;

    private Game game;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<RuleViolation> validate() {
        if (activeValidators == null || game.getState() != game.getPrevious().getState()) {
            System.out.println("game state: " + game.getPrevious().getState() + " -> " + game.getState());

            List<RuleValidator> validators = RULE_VALIDATORS.stream().filter((validator) -> validator.activeStates().contains(game.getState())).toList();

            List<RuleValidator> toReset = new ArrayList<>(validators);
            if (activeValidators != null) {
                toReset.removeAll(activeValidators);
            }

            toReset.forEach(RuleValidator::reset);
            activeValidators = validators;
        }

        List<RuleViolation> violations = new ArrayList<>();
        for (RuleValidator validator : RULE_VALIDATORS) {
            RuleViolation violation = validator.validate(game);
            if (violation != null) {
                violations.add(violation);
            }
        }

        return violations;
    }
}
