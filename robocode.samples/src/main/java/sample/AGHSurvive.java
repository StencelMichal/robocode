package sample;

import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.term.Trapezoid;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;
import java.awt.*;

public class AGHSurvive extends AdvancedRobot {
    boolean movingForward;

    private Engine engine;
    private InputVariable enemyDistance;
    private InputVariable myEnergy;
    private OutputVariable velocity;

    private void initializeFuzzyLogic(){
        FuzzyLite.setDebugging(true);
        // Inicjalizacja silnika rozmytego
        engine = new Engine();
        engine.setName("EnergyManagement");

        // Definicje zmiennych wejściowych
        enemyDistance = new InputVariable();
        enemyDistance.setName("enemyDistance");
        enemyDistance.setEnabled(true);
        enemyDistance.setRange(0, 1000.0);
        enemyDistance.addTerm(new Ramp("close", 0.0, 600));
        enemyDistance.addTerm(new Ramp("far", 600, 0));
        engine.addInputVariable(enemyDistance);


//        myEnergy = new InputVariable();
//        myEnergy.setName("myEnergy");
//        myEnergy.setEnabled(true);
//        myEnergy.setRange(0, 100);
//        myEnergy.addTerm(new Ramp("lowEnergy", 0.0, 40.0));
//        myEnergy.addTerm(new Ramp("highEnergy", 100.0, 30.0));
//        engine.addInputVariable(myEnergy);

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
        // Set colors
        setBodyColor(new Color(140, 130, 50));
        setGunColor(new Color(4, 150, 50));
        setRadarColor(new Color(30, 200, 15));
        setBulletColor(new Color(255, 123, 100));
        setScanColor(new Color(5, 200, 4));

        // Loop forever
        while (true) {
            // Tell the game we will want to move ahead 40000 -- some large number
            fullScan();
            setAhead(40000);
            movingForward = true;
            // Tell the game we will want to turn right 90
//            setTurnRight(90);
//            fullScan();
            // At this point, we have indicated to the game that *when we do something*,
            // we will want to move ahead and turn right.  That's what "set" means.
            // It is important to realize we have not done anything yet!
            // In order to actually move, we'll want to call a method that
            // takes real time, such as waitFor.
            // waitFor actually starts the action -- we start moving and turning.
            // It will not return until we have finished turning.
//            waitFor(new TurnCompleteCondition(this));
//            fullScan();
            // Note:  We are still moving ahead now, but the turn is complete.
            // Now we'll turn the other way...
//            setTurnLeft(180);
//            fullScan();
            // ... and wait for the turn to finish ...
//            waitFor(new TurnCompleteCondition(this));
//            fullScan();
            // ... then the other way ...
//            setTurnRight(180);
//            fullScan();
            // .. and wait for that turn to finish.
//            waitFor(new TurnCompleteCondition(this));
//            fullScan();

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
