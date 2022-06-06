/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop multi-agent systems in compliance with the FIPA specifications.
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

package org.example.meeting;

import jade.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;
import org.example.meeting.ontology.Appointment;
import org.example.meeting.ontology.MSOntology;
import org.example.meeting.ontology.Person;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

/**
 * @author Fabio Bellifemine - CSELT S.p.A
 * @version $Date: 2003-03-19 16:07:33 +0100 (mer, 19 mar 2003) $ $Revision: 3843 $
 */

public class MyFipaContractNetInitiatorBehaviour extends ContractNetInitiator {

    private final static long TIMEOUT = 60000; // 1 minute
    private final ACLMessage cfpMsg = new ACLMessage(ACLMessage.CFP);
    private final Appointment pendingApp;
    private final MeetingSchedulerAgent myAgent;


    public MyFipaContractNetInitiatorBehaviour(MeetingSchedulerAgent a, Appointment app, List group) {
        super(a, null);
        myAgent = a;
        // fill the fields of the cfp message
        cfpMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        cfpMsg.setOntology(MSOntology.NAME);
        cfpMsg.setReplyByDate(new Date(System.currentTimeMillis() + TIMEOUT));
        cfpMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        Iterator i = group.iterator();
        while (i.hasNext()) {
            cfpMsg.addReceiver((AID) i.next());
        }

        try {// fill the content
            myAgent.fillAppointment(cfpMsg, app);
        } catch (FIPAException e) {
            e.printStackTrace();
            myAgent.doDelete();
        }
        pendingApp = (Appointment) app.clone();
        //reset(cfpMsg); // updates the message to be sent
        System.out.println("myFipaContractNetInitiatorBehaviour msg:" + cfpMsg);
    }

    @Override
    protected Vector<ACLMessage> prepareCfps(ACLMessage cfp) {
        Vector<ACLMessage> v = new Vector<>(1);
        v.addElement(cfpMsg);
        return v;
    }

    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        System.err.println("!!! ContractNetInitiator handleNotUnderstood: " + msg.toString());
    }

    @Override
    protected void handleOutOfSequence(ACLMessage msg) {
        System.err.println("!!! ContractNetInitiator handleOutOfSequence: " + msg.toString());
    }

    @Override
    protected void handleRefuse(ACLMessage msg) {
        System.err.println("!!! ContractNetInitiator received Refuse: " + msg.toString());
    }

    @Override
    protected void handleAllResponses(Vector proposals, Vector retMsgs) {

        ACLMessage msg;
        ArrayList acceptableDates = new ArrayList();
        ArrayList acceptedDates;

        if (proposals.size() == 0)
            return;

        Calendar c = Calendar.getInstance();

        for (Iterator i = pendingApp.getAllPossibleDates(); i.hasNext(); ) {
            c.setTime((Date) i.next());
            acceptableDates.add(c.get(Calendar.DATE));

        }
        for (int i = 0; i < proposals.size(); i++) {
            msg = (ACLMessage) proposals.elementAt(i);
            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                acceptedDates = new ArrayList();

                try {
                    Appointment a = myAgent.extractAppointment(msg);
                    for (Iterator ii = a.getAllPossibleDates(); ii.hasNext(); ) {
                        c.setTime((Date) ii.next());
                        Integer day = c.get(Calendar.DATE);
                        if (acceptableDates.contains(day))
                            acceptedDates.add(day);
                    }
                    acceptableDates = (ArrayList) acceptedDates.clone();
                    if (msg.getReplyWith() != null)
                        msg.setInReplyTo(msg.getReplyWith());
                    msg.clearAllReceiver();
                    msg.addReceiver(msg.getSender());
                    msg.setSender(myAgent.getAID());
                    retMsgs.addElement(msg);
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            } // end if "propose"
        } // end of for proposals.size()

        ACLMessage replyMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        replyMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        replyMsg.setOntology(MSOntology.NAME);
        if (acceptableDates.size() > 0) {
            Date d = new Date();
            int dateNumber = (Integer) acceptableDates.get(0);
            c.set(Calendar.DATE, dateNumber);
            pendingApp.setFixedDate(c.getTime());
            try {
                myAgent.fillAppointment(replyMsg, pendingApp);
            } catch (FIPAException e) {
                e.printStackTrace();
                myAgent.doDelete();
            }
        } else
            replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);

        for (int i = 0; i < retMsgs.size(); i++) {
            ((ACLMessage) retMsgs.elementAt(i)).setPerformative(replyMsg.getPerformative());
            ((ACLMessage) retMsgs.elementAt(i)).setContent(replyMsg.getContent());
        }

    }

    @Override
    public void handleAllResultNotifications(Vector messages) {
        // I here receive failure or inform-done
        ACLMessage msg;
        boolean accepted = false;
        Person p;
        pendingApp.clearAllInvitedPersons();
        for (int i = 0; i < messages.size(); i++) {
            msg = (ACLMessage) messages.elementAt(i);
            if (msg.getPerformative() == ACLMessage.INFORM) {
                accepted = true;
                p = myAgent.getPersonByAgentName(msg.getSender());
                if (p == null)
                    p = new Person(msg.getSender().getName(), null, null);
                pendingApp.addInvitedPersons(p);
            }
        }
        if (accepted)
            myAgent.addMyAppointment(pendingApp);

    }
} 


    
