/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop
 multi-agent systems in compliance with the FIPA specifications.
 Copyright (C) 2000 CSELT S.p.A.

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation,
 version 2.1 of the License.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/

package org.example.meeting.ontology;

import jade.content.Concept;
import jade.core.AID;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.util.Date;

/**
 * @author Fabio Bellifemine - CSELT S.p.A
 * @version $Date: 2008-10-09 14:04:02 +0200 (gio, 09 ott 2008) $ $Revision: 6051 $
 */

public class Appointment implements Cloneable, Concept {

    private String description;
    private Date startingOn;
    private Date endingWith;
    private List invited = new ArrayList(); // Vector of Persons
    private List possibleDates = new ArrayList(); // Vector of Date
    private Date fixedDate;
    private AID invitingAgent;

    public Appointment() {
        startingOn = new Date();
        endingWith = new Date();
        description = "Unknown description";
    }

    public AID getInviter() {
        return invitingAgent;
    }

    public void setInviter(AID name) {
        invitingAgent = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String descr) {
        description = descr.replace(' ', '_');
    }

    public Date getStartingOn() {
        return startingOn;
    }

    public void setStartingOn(Date date) {
        startingOn = date;
    }

    public Date getEndingWith() {
        return endingWith;
    }

    public void setEndingWith(Date date) {
        endingWith = date;
    }

    public void addInvitedPersons(Person p) {
        invited.add(p);
    }

    public Iterator getAllInvitedPersons() {
        return invited.iterator();
    }

    public void clearAllInvitedPersons() {
        invited.clear();
    }

    public void addPossibleDates(Date d) {
        possibleDates.add(d);
    }

    public Iterator getAllPossibleDates() {
        return possibleDates.iterator();
    }

    public void clearAllPossibleDates() {
        possibleDates.clear();
    }


    public Date getFixedDate() {
        if (fixedDate == null)
            return startingOn;
        else return fixedDate;
    }

    public void setFixedDate(Date date) {
        fixedDate = date;
    }


    public synchronized Object clone() {
        Appointment result;
        try {
            result = (Appointment) super.clone();
            result.invited = (ArrayList) ((ArrayList) invited).clone();
            result.possibleDates = (ArrayList) ((ArrayList) possibleDates).clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // this should never happen
            result = null;
        }
        return result;
    }


    public void isValid() throws Exception {
        if (startingOn.after(endingWith))
            throw new Exception("Ending date must be before Starting Date");
        if (description.length() <= 0)
            throw new Exception("The Appointment must contain a description");
    }


    public String toString() {
        StringBuilder str = new StringBuilder("(Appointment ");
        if (description.length() > 0)
            str.append(":description \"").append(description).append("\" ");
        if (fixedDate == null) {
            str.append(":starting-on \"").append(startingOn.toString()).append("\" :ending-with \"").append(endingWith.toString()).append("\" ");
        } else str.append(":fixed-on \"").append(fixedDate).append("\" ");
        str.append(":invited (set ");
        for (int i = 0; i < invited.size(); i++)
            str.append("(").append(invited.get(i).toString()).append(") ");
        str.append(") ");
        str.append(":called-by ").append(invitingAgent.toString());
        str.append(" :possible-dates (set ");
        for (int i = 0; i < possibleDates.size(); i++)
            str.append("\"").append(possibleDates.get(i).toString()).append("\" ");
        str.append(")");
        return str + ")";
    }

}
