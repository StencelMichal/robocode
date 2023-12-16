package sample;

import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;
import java.awt.*;

public class AGHEnergyManagement extends AdvancedRobot {
    boolean movingForward;

    private Engine engine;
    private InputVariable enemyDistance;
    private InputVariable myEnergy;
    private InputVariable distance;
    private OutputVariable shootEnergy;

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
        enemyDistance.addTerm(new Ramp("close", 0.0, 400.0));
        enemyDistance.addTerm(new Ramp("far", 1000.0, 200.0));
        engine.addInputVariable(enemyDistance);


        myEnergy = new InputVariable();
        myEnergy.setName("myEnergy");
        myEnergy.setEnabled(true);
        myEnergy.setRange(0, 100);
        myEnergy.addTerm(new Ramp("lowEnergy", 0.0, 40.0));
        myEnergy.addTerm(new Ramp("highEnergy", 100.0, 30.0));
        engine.addInputVariable(myEnergy);

        shootEnergy = new OutputVariable();
        shootEnergy.setEnabled(true);
        shootEnergy.setName("shootEnergy");
        shootEnergy.setRange(1, Rules.MAX_BULLET_POWER);
        shootEnergy.fuzzyOutput().setAggregation(new DrasticSum());
        shootEnergy.setDefuzzifier(new WeightedAverage());
        shootEnergy.addTerm(new Ramp("shootLowEnergy", 0.0, Rules.MAX_BULLET_POWER));
        shootEnergy.addTerm(new Ramp("shootHighEnergy", Rules.MAX_BULLET_POWER, 0));

        engine.addOutputVariable(shootEnergy);

        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setEnabled(true);
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setDisjunction(null);
        ruleBlock.setImplication(new AlgebraicProduct());
        ruleBlock.setActivation(new General());

        ruleBlock.addRule(Rule.parse("if enemyDistance is close and myEnergy is lowEnergy then shootEnergy is shootLowEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is close and myEnergy is highEnergy then shootEnergy is shootHighEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far and myEnergy is lowEnergy then shootEnergy is shootLowEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far and myEnergy is highEnergy then shootEnergy is shootLowEnergy", engine));

        engine.addRuleBlock(ruleBlock);
    }

    public void run() {
        initializeFuzzyLogic();
        // Set colors
        setBodyColor(new Color(255, 0, 0));
        setGunColor(new Color(0, 150, 50));
        setRadarColor(new Color(117, 238, 238));
        setBulletColor(new Color(188, 0, 255));
        setScanColor(new Color(238, 177, 177));

        // Loop forever
        while (true) {
            // Tell the game we will want to move ahead 40000 -- some large number
            setAhead(40000);
            movingForward = true;
            // Tell the game we will want to turn right 90
            setTurnRight(90);
            // At this point, we have indicated to the game that *when we do something*,
            // we will want to move ahead and turn right.  That's what "set" means.
            // It is important to realize we have not done anything yet!
            // In order to actually move, we'll want to call a method that
            // takes real time, such as waitFor.
            // waitFor actually starts the action -- we start moving and turning.
            // It will not return until we have finished turning.
            waitFor(new TurnCompleteCondition(this));
            // Note:  We are still moving ahead now, but the turn is complete.
            // Now we'll turn the other way...
            setTurnLeft(180);
            // ... and wait for the turn to finish ...
            waitFor(new TurnCompleteCondition(this));
            // ... then the other way ...
            setTurnRight(180);
            // .. and wait for that turn to finish.
            waitFor(new TurnCompleteCondition(this));
            // then back to the top to do it all again
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
        myEnergy.setValue(getEnergy());

        engine.process();

        double calculatedShootEnergy = shootEnergy.getValue();
        if(!Double.isNaN(calculatedShootEnergy)){
            fire(calculatedShootEnergy);
        }
        else {
            System.out.println("dupa");
            fire(1);
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

