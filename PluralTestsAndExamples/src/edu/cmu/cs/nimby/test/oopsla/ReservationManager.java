/**
 * Copyright (C) 2007, 2008 Carnegie Mellon University and others.
 *
 * This file is part of Plural.
 *
 * Plural is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Plural is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Plural; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking Plural statically or dynamically with other modules is
 * making a combined work based on Plural. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of Plural
 * give you permission to combine Plural with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of Eclipse under the Eclipse Public
 * License (or modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the
 * GNU GPL for Plural and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of Plural are not
 * obligated to grant this special exception for their modified
 * versions; it is their choice whether to do so. The GNU General
 * Public License gives permission to release a modified version
 * without this exception; this exception also makes it possible to
 * release a modified version which carries forward this exception.
 */

package edu.cmu.cs.nimby.test.oopsla;


/*
 * This is my second version of the reservation manager from the paper.
 * It uses a better permission architecture. Originally this was the
 * main example from the paper, but it was removed because it was a little
 * too complicated.
 * 
 * 9/15/08
 * This file has changed since publication. It was been updated to use
 * our new permission annotations, which can be parsed from text. This
 * one actually ended up being a good deal harder than I had thought.
 */

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;

@PassingTest
@UseAnalyses("NIMBYChecker")
@ClassStates(@State(name="alive", inv="unique(rsvnDatabase)"))
class RsvnManager {
	private ReservationDatabase rsvnDatabase;

	/**
	 * Given a bus-based itinerary, this method will upgrade that
	 * itinerary to a air-based one if demand for bus tickets is
	 * high.
	 */
	@Share(use = Use.FIELDS)
	@ResultUnique
	Itinerary upgradeIfAvailable(
			@Unique(returned = false, requires="byLand") Itinerary old_itin,
			@Share Flight desired_flight,
			int cust_id) {
		atomic: {			
			ReservationDatabase rdb = rsvnDatabase;
			this.rsvnDatabase = null;
			
			if( rdb.isHighDemand() && desired_flight.seatAvail() ) {
				FlightRsvn new_flight = rdb.reserveSeatH(desired_flight, cust_id);
				BusRsvn old_bus = old_itin.getConfirmedBusRsvn();
				rdb.relinquish(old_bus);
				old_itin.makeByAir(new_flight);
				
				this.rsvnDatabase = rdb;
				//rdb = null;
				return old_itin;
			}
			else {
				return old_itin;
			}
		}
	}
}

/*
 * It's not over yet.
 */
@ClassStates({
	@State(name="alive", inv="full(myFlightRsvn) * full(myBusRsvn)"),
	@State(name="byAir", inv="myBusRsvn == null * myFlightRsvn in confirmed"),
    @State(name="byLand", inv="myFlightRsvn == null * myBusRsvn in confirmed"),
	@State(name="canceled", inv="myFlightRsvn == null * myBusRsvn == null")})
class Itinerary {
	
	@Perm(requires="unique(this!fr) in byLand", 
		  ensures="unique(this!fr) in canceled * full(result) in confirmed")
	BusRsvn getConfirmedBusRsvn() {
		BusRsvn r = this.myBusRsvn;
		this.myBusRsvn = null;
		return r;
	}
	
	@Pure(use = Use.FIELDS)
	void emailReminder() {
		if( myFlightRsvn.isConfirmed() ) {
			System.out.println("Reminder, Flight: " + myFlightRsvn.toString() );
		}
		else if( myBusRsvn.isConfirmed() ) {
			System.out.println("Reminder, Bus: " + myFlightRsvn.toString() );
		}
		return;
	}
	
	@Perm(requires="full(#0) in confirmed", ensures="unique(this!fr) in byLand")
	public Itinerary(BusRsvn myBusRsvn) {
		this.myBusRsvn = myBusRsvn;
		this.myFlightRsvn = null;
	}
	
	@Perm(requires="full(#0) in confirmed", ensures="unique(this!fr) in byAir")
	public Itinerary(FlightRsvn myFlightRsvn) {
		this.myFlightRsvn = myFlightRsvn;
		this.myBusRsvn = null;
	}
	
	FlightRsvn myFlightRsvn;
	BusRsvn myBusRsvn;
		
	@Unique(use = Use.FIELDS, ensures="byAir")
	void makeByAir(@Unique(requires="confirmed", returned=false) FlightRsvn new_flight) {
		this.myBusRsvn = null;
		this.myFlightRsvn = new_flight;
		return;
	}
}

class Flight {
	
	@Perm(ensures="unique(this!fr)")
	public Flight() {
		
	}
	
	@Pure(use = Use.FIELDS)
	@TrueIndicates("seatAvail")
	@FalseIndicates("filled")
	boolean seatAvail() {
		return true;
	}
}

class FlightRsvn {

	@Perm(ensures="unique(this!fr)")
	public FlightRsvn() {
		
	}
	
	@Pure(use = Use.FIELDS)
	@TrueIndicates("confirmed")
	@FalseIndicates("void")
	boolean isConfirmed() {
		return true;
	}
	
	@Full(use = Use.FIELDS, ensures="confirmed")
	void confirm() {}
}

class BusRsvn {

	@Perm(ensures="unique(this!fr)")
	public BusRsvn() {
		
	}
	
	@Pure(use = Use.FIELDS)
	@TrueIndicates("confirmed")
	@FalseIndicates("void")
	boolean isConfirmed() {
		return true;
	}
	
	@Full(use = Use.FIELDS, requires="confirmed", ensures="void")
	public void makeVoid() {
		return;
	}
	
}

class ReservationDatabase {

	@Imm(use = Use.FIELDS)
	@TrueIndicates("highBusDemand")
	boolean isHighDemand() {
		return true;
	}
	
	@Full(requires="highBusDemand")
	void relinquish(@Full(requires="confirmed", ensures="void") BusRsvn myBusRsvn) {
		myBusRsvn.makeVoid();
		return;
	}
	
	@Full
	@ResultUnique(ensures="confirmed")
	FlightRsvn reserveSeat(@Share(requires="seatAvail", ensures="alive") Flight f, int cid) {
		/*
		 * Dummy method.
		 */
		FlightRsvn result = new FlightRsvn();
		result.confirm();
		return result;
	}
	
	@Full(requires="highBusDemand", ensures="highBusDemand")
	@ResultUnique(ensures="confirmed")
	FlightRsvn reserveSeatH(@Share(requires="seatAvail", ensures="alive") Flight f, int cid) {
		/*
		 * Dummy method.
		 */
		FlightRsvn result = new FlightRsvn();
		result.confirm();
		return result;
	}	
}

