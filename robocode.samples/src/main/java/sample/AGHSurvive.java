package sample;

import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;

import java.awt.*;

public class AGHSurvive extends AdvancedRobot {
    boolean movingForward;

    private Engine engine;
    private InputVariable enemyDistance;
    private OutputVariable velocity;

    private void initializeFuzzyLogic(){
        FuzzyLite.setDebugging(true);
        engine = new Engine();
        engine.setName("EnergyManagement");

        enemyDistance = new InputVariable();
        enemyDistance.setName("enemyDistance");
        enemyDistance.setEnabled(true);
        enemyDistance.setRange(0, 1000.0);
        enemyDistance.addTerm(new Ramp("close", 0.0, 600));
        enemyDistance.addTerm(new Ramp("far", 600, 0));
        engine.addInputVariable(enemyDistance);

        velocity = new OutputVariable();
        velocity.setEnabled(true);
        velocity.setName("velocity");
        velocity.setRange(1.0, Rules.MAX_VELOCITY);
        velocity.fuzzyOutput().setAggregation(new DrasticSum());
        velocity.setDefuzzifier(new WeightedAverage());
        velocity.addTerm(new Ramp("slow", 1.0, Rules.MAX_VELOCITY));
        velocity.addTerm(new Ramp("fast", Rules.MAX_VELOCITY, 1.0));

        engine.addOutputVariable(velocity);

        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setEnabled(true);
        ruleBlock.setConjunction(null);
        ruleBlock.setDisjunction(null);
        ruleBlock.setImplication(new AlgebraicProduct());
        ruleBlock.setActivation(new General());

        ruleBlock.addRule(Rule.parse("if enemyDistance is close then velocity is fast", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far then velocity is slow", engine));

        engine.addRuleBlock(ruleBlock);
    }

    public void run() {
        initializeFuzzyLogic();
        setBodyColor(new Color(140, 130, 50));
        setGunColor(new Color(4, 150, 50));
        setRadarColor(new Color(30, 200, 15));
        setBulletColor(new Color(255, 123, 100));
        setScanColor(new Color(5, 200, 4));

        while (true) {
            fullScan();
            setAhead(40000);
            movingForward = true;
        }
    }

    private void fullScan(){
        double radarIncrement = 12;
        for (int i = 0; i < 30; i++) {
            turnGunLeft(radarIncrement);
        }
    }

    /**
     * onHitWall:  Handle collision with wall.
     */
    public void onHitWall(HitWallEvent e) {
        // Bounce off!
        reverseDirection();
    }

    /**
     * reverseDirection:  Switch from ahead to back &amp; vice versa
     */
    public void reverseDirection() {
        if (movingForward) {
            setBack(40000);
            movingForward = false;
        } else {
            setAhead(40000);
            movingForward = true;
        }
    }

    /**
     * onScannedRobot:  Fire!
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        enemyDistance.setValue(e.getDistance());
        engine.process();
        double newVelocity = velocity.getValue();
        setMaxVelocity(newVelocity);
        fire(1);

        double enemyHeading = e.getHeading();
        double desiredHeading = (enemyHeading + 90) % 360;
        double currentDegree = getHeading();
        double turnDegree = Math.abs(desiredHeading - currentDegree);
        if(desiredHeading > currentDegree){
            turnRight(turnDegree);
        } else {
            turnLeft(turnDegree);
        }
    }

    /**
     * onHitRobot:  Back up!
     */
    public void onHitRobot(HitRobotEvent e) {
        // If we're moving the other robot, reverse!
        if (e.isMyFault()) {
            reverseDirection();
        }
    }
}
