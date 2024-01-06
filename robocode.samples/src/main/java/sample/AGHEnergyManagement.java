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
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;

import java.awt.*;

public class AGHEnergyManagement extends AdvancedRobot {
    boolean movingForward;

    private Engine engine;
    private InputVariable enemyDistance;
    private InputVariable myEnergy;
    private OutputVariable shootEnergy;

    private void initializeFuzzyLogic(){
        FuzzyLite.setDebugging(true);
        engine = new Engine();
        engine.setName("EnergyManagement");

        enemyDistance = new InputVariable();
        enemyDistance.setName("enemyDistance");
        enemyDistance.setEnabled(true);
        enemyDistance.setRange(0, 1000.0);
        enemyDistance.addTerm(new Ramp("close", 0.0, 400.0));
        enemyDistance.addTerm(new Trapezoid("far", 1000, 999, 500, 200));
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
        shootEnergy.addTerm(new Ramp("shootLowEnergy",  1.0, Rules.MAX_BULLET_POWER));
        shootEnergy.addTerm(new Trapezoid("shootHighEnergy", Rules.MAX_BULLET_POWER, Rules.MAX_BULLET_POWER, 2.0, 1.0));

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
        setBodyColor(new Color(255, 0, 0));
        setGunColor(new Color(0, 150, 50));
        setRadarColor(new Color(117, 238, 238));
        setBulletColor(new Color(188, 0, 255));
        setScanColor(new Color(238, 177, 177));

        while (true) {
            setAhead(40000);
            movingForward = true;
            setTurnRight(90);
            waitFor(new TurnCompleteCondition(this));
            setTurnLeft(180);
            waitFor(new TurnCompleteCondition(this));
            setTurnRight(180);
            waitFor(new TurnCompleteCondition(this));
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
            fire(3-calculatedShootEnergy);
        }
        else {
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

