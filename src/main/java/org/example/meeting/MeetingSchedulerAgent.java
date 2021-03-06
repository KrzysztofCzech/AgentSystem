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


package org.example.meeting;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;
import org.example.meeting.ontology.Appointment;
import org.example.meeting.ontology.MSOntology;
import org.example.meeting.ontology.Person;

import java.util.*;

/**
 * @author Fabio Bellifemine - CSELT S.p.A
 * @version $Date: 2004-07-19 17:09:07 +0200 (lun, 19 lug 2004) $ $Revision: 5215 $
 */
public class MeetingSchedulerAgent extends GuiAgent {

    /**
     * GuiEvent to start the agent activity
     **/
    final static int STARTTASKS = 2;
    final static int FIXAPPOINTMENT = 3;
    final static int REGISTERWITHDF = 4;
    final static int CANCELAPPOINTMENT = 5;
    final static int SEARCHWITHDF = 6;
    private final Vector<AID> knownDF = new Vector<>(); // list of known DF with which the agent is registered
    private final Hashtable<String, Person> knownPersons = new Hashtable<>(); // list of known persons: (String)name -> Person
    private final Hashtable<String, Appointment> appointments = new Hashtable<>(); // list of appointments:  (String)date -> Appointment
    private final Ontology MSOnto = MSOntology.getInstance();
    private final Codec SL0Codec = new SLCodec();
    private final Logger logger = Logger.getMyLogger(this.getClass().getName());
    private String user; // this is the name of the current user
    private MainFrame mf; // pointer to the main frame of the GUI
    private ACLMessage cancelMsg;

    @Override
    protected void setup() {
        // fill the fields of the cancel message
        cancelMsg = new ACLMessage(ACLMessage.CANCEL);
        cancelMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        cancelMsg.setOntology(MSOntology.NAME);

        // register the ontology and the content language
        getContentManager().registerOntology(MSOnto);
        getContentManager().registerLanguage(SL0Codec, FIPANames.ContentLanguage.FIPA_SL0);
        // request the user to insert the password and username
        (new PasswordDialog(this, this.getLocalName())).setVisible(true);
        // when the user has inserted password and username, a GuiEvent will be posted
    }

    /**
     * This method is executed by the Agent thread when the GUI
     * posts a new Event
     **/
    @Override
    protected void onGuiEvent(GuiEvent ev) {
        switch (ev.getType()) {
            case STARTTASKS -> {
                String userName = (String) ev.getParameter(0);
                startTasks(userName);
            }
            case FIXAPPOINTMENT -> {
                Appointment a = (Appointment) ev.getParameter(0);
                fixAppointment(a);
            }
            case REGISTERWITHDF -> {
                AID dfName = new AID((String) ev.getParameter(0), AID.ISGUID);
                try {
                    mf.showErrorMessage("Registering with " + dfName.getName() + " ...");
                    if (logger.isLoggable(Logger.CONFIG))
                        logger.log(Logger.CONFIG, "Registering with " + dfName.getName() + " ...");
                    DFService.register(this, dfName, getDFAgentDescription());
                    mf.showErrorMessage("Done registration with " + dfName.getName() + ".");
                    if (logger.isLoggable(Logger.CONFIG))
                        logger.log(Logger.CONFIG, "Done registration with " + dfName.getName() + ".");
                } catch (FIPAException e) {
                    e.printStackTrace();
                    mf.showErrorMessage(e.getMessage());
                    if (logger.isLoggable(Logger.WARNING))
                        logger.log(Logger.WARNING, e.getMessage());
                }
            }
            case CANCELAPPOINTMENT -> {
                Date d = (Date) ev.getParameter(0);
                cancelAppointment(d);
            }
            case SEARCHWITHDF -> {
                AID dfName2 = (AID) ev.getParameter(0);
                mf.showErrorMessage("Updating with DF: " + dfName2.getName() + " ...");
                if (logger.isLoggable(Logger.WARNING))
                    logger.log(Logger.WARNING, "Updating with DF: " + dfName2.getName() + " ...");
                searchPerson(dfName2, null);
            }
            default -> System.err.println("Received unexpexcted GuiEvent of type:" + ev.getType());
        }
    }

    /**
     * starts all the tasks of this agent
     **/
    private void startTasks(String userName) {
        setUser(userName);
        mf = new MainFrame(this, getUser() + " - Appointment Scheduler");
        mf.setVisible(true);
        try {
            DFService.register(this, getDFAgentDescription());
            knownDF.add(getDefaultDF());
            addKnownPerson(new Person(getUser(), getAID(), getDefaultDF()));
        } catch (FIPAException e) {
            e.printStackTrace();
            mf.showErrorMessage(e.getMessage());
            if (logger.isLoggable(Logger.WARNING))
                logger.log(Logger.WARNING, e.getMessage());

        }
        addBehaviour(new MyFipaContractNetResponderBehaviour(this));
        addBehaviour(new CancelAppointmentBehaviour(this));
    }


    protected void searchPerson(AID dfname, String personName) {
        ServiceDescription sd = new ServiceDescription();
        sd.setType("personal-agent");
        if (personName != null)
            sd.setOwnership(personName);
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addOntologies("pa-ontology");
        dfd.addServices(sd);
        try {
            DFAgentDescription[] l = DFService.search(this, dfname, dfd);

            if (l != null)
                for (DFAgentDescription dfAgentDescription : l) {
                    // add values to knownPersons
                    dfd = dfAgentDescription;
                    Person prs = new Person(((ServiceDescription) dfd.getAllServices().next()).getOwnership(), dfd.getName(), dfname);
                    addKnownPerson(prs);
                }
        } catch (FIPAException fe) {
            fe.printStackTrace();
            mf.showErrorMessage(fe.getMessage());
            if (logger.isLoggable(Logger.WARNING))
                logger.log(Logger.WARNING, fe.getMessage());
        }
    }


    /**
     * @return the description of this agent to be registered with the DF
     **/
    private DFAgentDescription getDFAgentDescription() {
        ServiceDescription sd = new ServiceDescription();
        sd.setName("AppointmentScheduling");
        sd.setType("personal-agent");
        sd.addOntologies("pa-ontology");
        sd.setOwnership(user);
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);
        dfd.addOntologies("pa-ontology");
        dfd.addProtocols("fipa-request fipa-Contract-Net");
        return dfd;
    }

    /**
     * This method returns an Enumeration of String.
     * Each String is the name of a DF with which the agent has been registered
     */
    protected Enumeration<AID> getKnownDF() {
        return knownDF.elements();
    }

    /**
     * This method returns an Enumeration of objects.
     * The object type is Person
     */
    protected Enumeration<Person> getKnownPersons() {
        return knownPersons.elements();
    }

    protected void addKnownPerson(Person p) {
        knownPersons.put(p.getName(), p);
        mf.showErrorMessage("Known " + p.getName());
        if (logger.isLoggable(Logger.CONFIG))
            logger.log(Logger.CONFIG, "Known " + p.getName());
    }

    protected Person getPerson(String name) {
        return knownPersons.get(name);
    }

    protected Person getPersonByAgentName(AID agentName) {
        Enumeration<Person> e = knownPersons.elements();
        Person p;
        while (e.hasMoreElements()) {
            p = e.nextElement();
            if (p.getAID().equals(agentName))
                return p;
        }
        return null;
    }


    /**
     * this method is called after a GUIEvent is received
     */
    private void fixAppointment(Appointment a) {

        List ag = new ArrayList();
        for (Iterator i = a.getAllInvitedPersons(); i.hasNext(); )
            ag.add(((Person) i.next()).getAID());

        if (ag.size() == 0) {//there is only me, nobody invited. Just find a free day
            Date goodDate = findADate(a);
            if (goodDate == null) {
                mf.showErrorMessage("No free date for " + a);
                if (logger.isLoggable(Logger.WARNING))
                    logger.log(Logger.WARNING, "No free date for " + a);
            } else {
                a.setFixedDate(goodDate);
                addMyAppointment(a);
            }
        } else {
            if (fillAppointmentWithPossibleDates(a))
                // if returns True than there are available dates
                addBehaviour(new MyFipaContractNetInitiatorBehaviour(this, a, ag));
            else {
                mf.showErrorMessage("No free date for " + a);
                if (logger.isLoggable(Logger.WARNING))
                    logger.log(Logger.WARNING, "No free date for " + a);
            }
        }
    }


    /**
     * This function fills the Appointment with all the dates when this user
     * is available
     *
     * @param a the Appointment
     * @return true if there was at least one day available, false otherwise
     */
    private boolean fillAppointmentWithPossibleDates(Appointment a) {
        Date ds = (Date) a.getStartingOn().clone();
        Date de = a.getEndingWith();
        while (!ds.after(de)) {
            if (!appointments.containsKey(key(ds)))
                a.addPossibleDates((Date) ds.clone());
            ds.setTime(ds.getTime() + (24 * 60 * 60 * 1000)); // + 1 day
        }
        return a.getAllPossibleDates().hasNext();
    }

    /**
     * @param a an Appointment
     * @return a good Date for that Appointment
     */
    Date findADate(Appointment a) {
        Date ds = a.getStartingOn();
        if (appointments.containsKey(key(ds))) {
            ds.setTime(ds.getTime() + (24 * 60 * 60 * 1000)); // + 1 day
            Date de = a.getEndingWith();

            while (!ds.after(de)) {
                if (appointments.containsKey(key(ds))) {
                    ds.setTime(ds.getTime() + (24 * 60 * 60 * 1000)); // + 1 day
                } else return ds;
            } // end of while
            return null;
        }
        return ds;
    }


    boolean isFree(Date d) {
        return !appointments.containsKey(key(d));
    }

    private Appointment getMyAppointment(Appointment app) {
        return appointments.get(key(app.getFixedDate()));
    }


    /**
     * This method is called after a GuiEvent is received.
     **/
    void cancelAppointment(Date d) {
        Appointment a = getMyAppointment(d);
        if (a == null) {
            mf.showErrorMessage("Nothing to cancel: no appointmen was fixed on " + d.toString());
            if (logger.isLoggable(Logger.WARNING))
                logger.log(Logger.WARNING, "Nothing to cancel: no appointmen was fixed on " + d);
            return;
        }

        // set the receivers of the message
        cancelMsg.clearAllReceiver();
        for (Iterator i = a.getAllInvitedPersons(); i.hasNext(); )
            cancelMsg.addReceiver(((Person) i.next()).getAID());

        try {
            if (a.getAllInvitedPersons().hasNext()) { // there was at least a receiver
                fillAppointment(cancelMsg, a);
                send(cancelMsg);
            }
            removeMyAppointment(a);
        } catch (FIPAException e) {
            e.printStackTrace();
            mf.showErrorMessage(e.getMessage());
            if (logger.isLoggable(Logger.WARNING))
                logger.log(Logger.WARNING, e.getMessage());
            //System.out.println(cancelMsg.toString());
        }
    }


    void removeMyAppointment(Appointment app) {
        Appointment a = getMyAppointment(app);
        if (a == null) {
            mf.showErrorMessage("Someone has requested to cancel an appointment for " + app.getFixedDate().toString() + " but there was no appointment actually");
            if (logger.isLoggable(Logger.WARNING))
                logger.log(Logger.WARNING, "Someone has requested to cancel an appointment for " + app.getFixedDate().toString() + " but there was no appointment actually");
        } else {
            appointments.remove(key(a.getFixedDate()));
            mf.showErrorMessage("Cancelled Appointment: " + a);
            if (logger.isLoggable(Logger.CONFIG))
                logger.log(Logger.CONFIG, "Cancelled Appointment: " + a);
        }
    }

    // called by myFipaContractNetInitiatorBehaviour
    void addMyAppointment(Appointment a) {
        appointments.put(key(a.getFixedDate()), a);
        mf.calendar1_Action();
        mf.showErrorMessage(a.toString());
        if (logger.isLoggable(Logger.CONFIG))
            logger.log(Logger.CONFIG, a.toString());
    }


    /**
     * This function return the key to be used in the Hashtable
     */
    private String key(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return "" + c.get(Calendar.YEAR) + c.get(Calendar.MONTH) + c.get(Calendar.DAY_OF_MONTH);
    }

    // it used by mainFrame
    Appointment getMyAppointment(Date date) {
        return appointments.get(key(date));
    }

    protected String getUser() {
        return user;
    }

    protected void setUser(String username) {
        user = username;
    }

    /**
     * this method extract an appointment data structure from a message
     **/
    Appointment extractAppointment(ACLMessage msg) throws FIPAException {
        try {
            ContentElement l = getContentManager().extractContent(msg);
            Action a = (Action) l;
            return (Appointment) a.getAction();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * this method fills a message with an appointment
     **/
    void fillAppointment(ACLMessage msg, Appointment app) throws FIPAException {
        List l = new ArrayList(1);
        Action a = new Action();
        a.setActor(getAID());
        a.setAction(app);
        l.add(a);
        try {
            getContentManager().fillContent(msg, a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} // end Agent.java

