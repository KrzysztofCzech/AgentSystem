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


package org.example;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.example.ontology.Appointment;


/**
 * This behaviour serves all CANCEL messages received by the agent.
 *
 * @author Fabio Bellifemine - CSELT S.p.A
 * @version $Date: 2003-03-19 16:07:33 +0100 (mer, 19 mar 2003) $ $Revision: 3843 $
 */
public class CancelAppointmentBehaviour extends CyclicBehaviour {

    private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
    private final MeetingSchedulerAgent myAgent;

    CancelAppointmentBehaviour(MeetingSchedulerAgent a) {
        super(a);
        myAgent = a;
    }

    @Override
    public void action() {
        ACLMessage cancel = myAgent.receive(mt);
        if (cancel == null) {
            block();
            return;
        }
        //System.err.println("CancelAppointmentBehaviour: received "+cancel.toString());
        try {
            Appointment app = myAgent.extractAppointment(cancel);
            if (app.getInviter().equals(myAgent.getAID()))
                // I called the appointment and I have to inform other agents of that
                myAgent.cancelAppointment(app.getFixedDate());
            else
                myAgent.removeMyAppointment(app);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}


