package com.example.wot_servient.la_mqtt.lamqtt.common;

import android.location.Location;

public class Direction {

	private final Position source;
	private final Position dest;
	private final Position cPosition;
	private double mCoefficient;
	private double nCoefficient;
	private boolean destReached;

	public Direction(Position source, Position destination, double speed) {
		this.source = source;
		this.dest = destination;
		this.cPosition = new Position(source.latitude, source.longitude);
		this.destReached = false;
		this.computeCoefficients();
	}

	public static double computeEDistance(double lat1, double long1, double lat2, double long2) {
		double latE = Math.pow(Math.abs(lat1 - lat2), 2);
		double longE = Math.pow(Math.abs(long1 - long2), 2);
		return Math.sqrt(latE + longE);
	}

	public static double computeDistanceGPS(double lat1, double lon1, double lat2, double lon2) {
		Location locationA = new Location("A");
		locationA.setLatitude(lat1);
		locationA.setLongitude(lon1);
		Location locationB = new Location("B");
		locationB.setLatitude(lat2);
		locationB.setLongitude(lon2);
		return locationA.distanceTo(locationB);
	}

	public void computeCoefficients() {
		this.mCoefficient = (this.source.latitude - this.dest.longitude) / (this.source.latitude - this.dest.latitude);
		this.nCoefficient = this.source.longitude - this.mCoefficient * this.source.latitude;
	}

	public Boolean isDestinationReached() {
		return this.destReached;
	}

	public double computeDistanceToNextGoal() {
		return Direction.computeDistanceGPS(this.cPosition.latitude, this.cPosition.longitude, this.dest.latitude, this.dest.longitude);
	}

	public Position computeAdvance(double speed, double timeAdvance) {
		double distanceToGoal = Direction.computeDistanceGPS(this.source.latitude, this.source.longitude, this.dest.latitude, this.dest.longitude);
		double distanceNow = Direction.computeDistanceGPS(this.cPosition.latitude, this.cPosition.longitude, this.dest.latitude, this.dest.longitude);
		double distanceE = Direction.computeEDistance(this.source.latitude, this.source.longitude, this.dest.latitude, this.dest.longitude);
		double realAdvance = speed * timeAdvance;
		double advance = realAdvance * distanceE / distanceToGoal;
		if ((distanceNow <= realAdvance)) {
			Position p = new Position(this.dest.latitude, this.dest.longitude);
			this.cPosition.latitude = this.dest.latitude;
			this.cPosition.longitude = this.dest.longitude;
			this.destReached = true;
			return p;
		} else {
			double newX = this.cPosition.latitude;
			double val = (advance * Math.sqrt(1 / (1 + this.mCoefficient * this.mCoefficient)));
			if (newX > this.dest.latitude) {
				newX = newX - val;
			} else {
				newX = newX + val;
			}
			double newY = newX * this.mCoefficient + this.nCoefficient;
			this.cPosition.latitude = newX;
			this.cPosition.longitude = newY;
			return new Position(newX, newY);
		}
	}
}
