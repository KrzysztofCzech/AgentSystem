
CLASSPATH ?= JADE-bin-4.5.0/jade/lib/jade.jar:target/szia-1.0-SNAPSHOT.jar:jars/CalendarBean.jar

.PHONY: build-jar
build-jar:
	mvn clean compile jar:jar

.Phony: run
run: build-jar
	java -cp ${CLASSPATH} jade.Boot -gui -nomtp meetings:org.example.MeetingSchedulerAgent
