package net.vveitas.offernet

import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Simulation {
	OfferNet on;
	Logger logger;

	private Simulation() {
		def config = new ConfigSlurper().parse(new File('configs/log4j-properties.groovy').toURL())
		PropertyConfigurator.configure(config.toProperties())
		logger = LoggerFactory.getLogger('Simulation.class');

		on = new OfferNet();
		on.flushVertices();
	}

	public one() {
		def chains = [Utils.createChain(3),Utils.createChain(2)]
		def agentList = this.createAgentNetwork(10,20,chains);
		this.connectIfSimilarForAllAgents(agentList,8,2);
		//search Cycles (not implemented yet)

	}

	private List createAgentNetwork(Integer numberOfAgents, Integer numberOfRandomWorks, ArrayList chains) {

		def agentList = on.createAgentNetwork(numberOfAgents)
		agentList.each {agent ->
			agent.ownsWork()
		}
		on.addRandomWorksToAgents(numberOfRandomWorks)
		chains.each {chain ->
			on.addChainToNetwork(chain)
		}

		return agentList;

	}

	private void connectIfSimilarForAllAgents(List agentList, Integer similarityThreshold, Integer maxDistance) {
		logger.warn("Searching and connecting similar items of all agents in the graph:")
		def newConnectionsCreated = 0;
		agentList.each {agent ->
			 newConnectionsCreated += agent.searchAndConnect(similarityThreshold,maxDistance);
		}
		logger.warn("Created {} new 'distance' links between items in the graph",newConnectionsCreated)
	}




}