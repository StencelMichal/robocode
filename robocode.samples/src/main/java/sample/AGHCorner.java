/*
 * Copyright (c) 2001-2023 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.activation.Highest;
import com.fuzzylite.activation.Lowest;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.norm.TNorm;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.AdvancedRobot;
import robocode.DeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;

import static java.lang.Double.NaN;
import static robocode.util.Utils.normalRelativeAngleDegrees;

/**
 * Corners - a sample robot by Mathew Nelson.
 * <p>
 * This robot moves to a corner, then swings the gun back and forth.
 * If it dies, it tries a new corner in the next round.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class AGHCorner extends AdvancedRobot {
	int others; // Number of other robots in the game
	static int corner = 0; // Which corner we are currently using
	// static so that it keeps it between rounds.
	boolean stopWhenSeeRobot = false; // See goCorner()
	boolean isInCorner = false;
	boolean isResettingPosition = false;

	private Engine engine;
	private InputVariable movingDirection;
	private InputVariable speed;
	private InputVariable distance;
	private OutputVariable shootDirection;

	private void initializeFuzzyLogic(){
		FuzzyLite.setDebugging(true);
		// Inicjalizacja silnika rozmytego
		engine = new Engine();
		engine.setName("ShootingDirection");

		// Definicje zmiennych wejściowych
		movingDirection = new InputVariable();
		movingDirection.setName("movingDirection");
		movingDirection.setEnabled(true);
		movingDirection.setRange(-45.0, 315.0);
		movingDirection.addTerm(new Triangle("movingLeft", -45, 135));
		movingDirection.addTerm(new Triangle("movingRight", 135, 315));
		engine.addInputVariable(movingDirection);


		speed = new InputVariable();
		speed.setName("speed");
		speed.setEnabled(true);
		speed.setRange(0, Rules.MAX_VELOCITY);
		speed.addTerm(new Ramp("movingSlow", 0, Rules.MAX_VELOCITY / 2 + 2 ));
		speed.addTerm(new Ramp("movingFast", Rules.MAX_VELOCITY / 2 - 2, Rules.MAX_VELOCITY));
		engine.addInputVariable(speed);

		distance = new InputVariable();
		distance.setName("distance");
		distance.setEnabled(true);
		distance.setRange(0.0, 1000.0);
		distance.addTerm(new Ramp("far", 200, 1000.0)); // TODO być może 1000 - 200 itp itd
		distance.addTerm(new Ramp("close", 0, 400));
		engine.addInputVariable(distance);

		shootDirection = new OutputVariable();
		shootDirection.setEnabled(true);
		shootDirection.setName("shootDirection");
		shootDirection.setRange(-45.0, 45.0);
		shootDirection.fuzzyOutput().setAggregation(new DrasticSum());
		shootDirection.setDefuzzifier(new Centroid(90));
		shootDirection.addTerm(new Ramp("shootFarRight", 45.0, 10));
		shootDirection.addTerm(new Ramp("shootRight", 25.0, 0));
		shootDirection.addTerm(new Ramp("shootLeft", -25.0, 0));
		shootDirection.addTerm(new Ramp("shootFarLeft", -45.0, -10.0));

//		hargaLele.setEnabled(true);
//		hargaLele.setRange(100000,1500000);
//		hargaLele.setDefaultValue(Double.NaN);
//		hargaLele.addTerm(new Trapezoid("MURAH",200000,500000));
//		hargaLele.addTerm(new Triangle("SEDANG",400000,1000000));
//		hargaLele.addTerm(new Triangle("MAHAL",800000,1500000));
//		hargaLele.fuzzyOutput().setAccumulation(new DrasticSum());
//		hargaLele.setDefuzzifier(new Centroid(1500000));

		engine.addOutputVariable(shootDirection);

		// Zasady
		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setEnabled(true);
		ruleBlock.setConjunction(new Minimum());
		ruleBlock.setDisjunction(null);
		ruleBlock.setImplication(new AlgebraicProduct());
		ruleBlock.setActivation(new General());

		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingSlow and distance is far then shootDirection is shootFarLeft", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingSlow and distance is close then shootDirection is shootLeft", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingFast and distance is far then shootDirection is shootFarLeft", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingFast and distance is close then shootDirection is shootLeft", engine));

		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingSlow and distance is far then shootDirection is shootFarRight", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingSlow and distance is close then shootDirection is shootRight", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingFast and distance is far then shootDirection is shootFarRight", engine));
		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingFast and distance is close then shootDirection is shootRight", engine));

//		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingSlow and distance is close then shootDirection is shootFarLeft", engine));
//		ruleBlock.addRule(Rule.parse("if movingDirection is movingLeft and speed is movingFast and distance is close then shootDirection is shootFarLeft", engine));
//
//		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingSlow and distance is close then shootDirection is shootFarRight", engine));
//		ruleBlock.addRule(Rule.parse("if movingDirection is movingRight and speed is movingFast and distance is close then shootDirection is shootFarRight", engine));




		// Dodaj więcej zasad...

		// Dodaj zmienne i zasady do silnika
		engine.addRuleBlock(ruleBlock);
	}

	/**
	 * run:  Corners' main run function.
	 */
	public void run() {

		initializeFuzzyLogic();
		// Set colors
		setBodyColor(Color.BLUE);
		setGunColor(Color.black);
		setRadarColor(Color.yellow);
		setBulletColor(Color.green);
		setScanColor(Color.green);

		// Save # of other bots
		others = getOthers();

		// Move to a corner
		goCorner();

		// Initialize gun turn speed to 3
		int gunIncrement = 3;

		// Spin gun back and forth
		while (true) {
			for (int i = 0; i < 30; i++) {
				turnGunLeft(gunIncrement);
			}
//			gunIncrement *= -1;
			isResettingPosition = true;
			turnGunRight(Math.abs(180.0 - getGunHeading()));
			isResettingPosition = false;
		}
	}

	/**
	 * goCorner:  A very inefficient way to get to a corner.  Can you do better?
	 */
	public void goCorner() {
		// We don't want to stop when we're just turning...
		stopWhenSeeRobot = false;
		// turn to face the wall to the "right" of our desired corner.
		turnRight(normalRelativeAngleDegrees(corner - getHeading()));
		// Ok, now we don't want to crash into any robot in our way...
		stopWhenSeeRobot = true;
		// Move to that wall
		ahead(5000);
		// Turn to face the corner
		turnLeft(90);
		// Move to the corner
		ahead(5000);
		// Turn gun to starting point
		turnGunLeft(90);
		isInCorner = true;
	}

	/**
	 * onScannedRobot:  Stop and fire!
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		if (isInCorner == false || isResettingPosition == true) {
			return;
		}
		// Przykładowe ustawienie wartości
		movingDirection.setValue(e.getHeading() - 45.0); // Ustaw wartość bazując na danych z e
		speed.setValue(Math.abs(e.getVelocity()) / 8); // Ustaw wartość bazując na danych z e
		distance.setValue(e.getDistance());
		// Wykonaj obliczenia
		engine.process();

		// Pobierz wynik i podejmij działanie
		double direction = shootDirection.getValue();
		// Użyj wartości kierunek do sterowania strzelaniem
		if(!Double.isNaN(direction)){
			double gunHeading = getGunHeading();

			double o_ile_mozna_w_prawo = 180.0 - gunHeading;
			double o_ile_mozna_w_lewo = 90.0 - gunHeading;

			double newDirection = Math.max(o_ile_mozna_w_lewo, Math.min(direction, o_ile_mozna_w_prawo));


//		double turnGunResult = gunHeading + direction;
//		double newDirection = direction;
//		if(turnGunResult > 180.0 || turnGunResult < 90.0) {
//			if(direction >= 0) {
//				newDirection = direction - (turnGunResult - 180.0);
//			} else {
//				newDirection = direction + (90.0 - turnGunResult);
//			}
//		}

			turnGunRight(newDirection);

			if (getGunHeading() < 90 || getGunHeading() > 180) {
				System.out.println(o_ile_mozna_w_prawo + ", " + o_ile_mozna_w_lewo + ", " + gunHeading + ", " + direction + ", " + newDirection);
			}

			// Should we stop, or just fire?
			if (stopWhenSeeRobot) {
				// Stop everything!  You can safely call stop multiple times.
//			stop();
				// Call our custom firing method
				smartFire(e.getDistance());

				// Look for another robot.
				// NOTE:  If you call scan() inside onScannedRobot, and it sees a robot,
				// the game will interrupt the event handler and start it over
				scan();
				// We won't get here if we saw another robot.
				// Okay, we didn't see another robot... start moving or turning again.
				resume();
			} else {
				smartFire(e.getDistance());

			}
		}


	}

	/**
	 * smartFire:  Custom fire method that determines firepower based on distance.
	 *
	 * @param robotDistance the distance to the robot to fire at
	 */
	public void smartFire(double robotDistance) {
		if (robotDistance > 200 || getEnergy() < 15) {
			fire(1);
		} else if (robotDistance > 50) {
			fire(2);
		} else {
			fire(3);
		}
	}

	/**
	 * onDeath:  We died.  Decide whether to try a different corner next game.
	 */
	public void onDeath(DeathEvent e) {
		// Well, others should never be 0, but better safe than sorry.
		if (others == 0) {
			return;
		}

		// If 75% of the robots are still alive when we die, we'll switch corners.
//		if (getOthers() / (double) others >= .75) {
//			corner += 90;
//			if (corner == 270) {
//				corner = -90;
//			}
//			out.println("I died and did poorly... switching corner to " + corner);
//		} else {
//			out.println("I died but did well.  I will still use corner " + corner);
//		}
	}
}
