package net.vveitas.offernet

import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Simulation {
	OfferNet on;
	Logger logger;
	List agentList;

	private Simulation() {

		def start = System.currentTimeMillis();
		def config = new ConfigSlurper().parse(new File('configs/log4j-properties.groovy').toURL())
		PropertyConfigurator.configure(config.toProperties())
		logger = LoggerFactory.getLogger('Simulation.class');

		on = new OfferNet();
		on.flushVertices();
		logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

	}

	private void createAgentNetworkWithChains(String[] args){
   		def numberOfAgents = args[1];
   		def numberOfRandomWorks = args[2];
	    def numberOfChains = args[3];
	    def lenghtOfChain = args[4];
	    List chains = [];
	    numberOfChains.times {
	    	chains.add(Utils.createChain(lenghtOfChain));
	    }
   		createAgentNetwork(numberOfAgents,numberOfRandomWorks,chains);
	}

	private List createAgentNetwork(Integer numberOfAgents, Integer numberOfRandomWorks, ArrayList chains) {

		def start = System.currentTimeMillis();
		agentList = on.createAgentNetwork(numberOfAgents)
		agentList.each {agent ->
			agent.ownsWork()
		}
		on.addRandomWorksToAgents(numberOfRandomWorks)
		chains.each {chain ->
			on.addChainToNetwork(chain)
		}
		logger.warn("Created agentNetwork with {} agents, {} randomWorks and {} chains",numberOfAgents,numberOfRandomWorks,chains.size())
		logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

		return agentList;
	}

	private Integer connectIfSimilarForAllAgents(List agentList, Integer similarityThreshold, Integer maxReachDistance) {

		def start = System.currentTimeMillis();
		logger.warn("Searching and connecting similar items of all agents in the graph:")
		def newConnectionsCreated = 0;
		agentList.each {agent ->
			 newConnectionsCreated += agent.searchAndConnect(similarityThreshold,maxReachDistance);
		}
		logger.warn("Created {} new 'similarity' links between items in the graph",newConnectionsCreated)
		logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

		return newConnectionsCreated
	}

	private void testNetworkSmall() {
	    def start = System.currentTimeMillis();
	    def chain = ["0010","0110","0000","1110"]
	    //def chain = Utils.createChain(4)
	    logger.info("Created chain {}", chain)

   	    def agent1 = new Agent(on.session);
	    def agent2 = new Agent(on.session);
	    def agent3 = new Agent(on.session);
   	    def agent4 = new Agent(on.session);
   	    logger.info("Created agents: {}",[agent1.id(),agent2.id(),agent3.id(),agent4.id()])


	    def work1 = agent1.ownsWork(chain[0],chain[1]);
	    logger.info("agent {} owns work {}", agent1,work1)
	    def work2 = agent2.ownsWork(chain[1],chain[2]);
	    logger.info("agent {} owns work {}", agent2,work2)
	    def work3 = agent3.ownsWork(chain[2],chain[3]);
	    logger.info("agent {} owns work {}", agent3,work3)
	    logger.info("Created works: {}",[work1.getId(),work2.getId(),work3.getId()])

	    agent1.knowsAgent(agent2);
	    logger.info("agent {} knows agent {}",agent1,agent2)
	    agent2.knowsAgent(agent3);
	    logger.info("agent {} knows agent {}",agent2,agent3)
	    agent3.knowsAgent(agent4);
	    logger.info("agent {} knows agent {}",agent3,agent4)

	    agentList = [agent1,agent2,agent3,agent4]
    }


	private void testNetworkSmallWithCycle() {
	    def start = System.currentTimeMillis();
	    def chain = ["0010","0110","0000","1110"]
	    //def chain = Utils.createChain(4)
	    logger.info("Created chain {}", chain)

   	    def agent1 = new Agent(on.session);
	    def agent2 = new Agent(on.session);
	    def agent3 = new Agent(on.session);
   	    def agent4 = new Agent(on.session);
   	    logger.info("Created agents: {}",[agent1.id(),agent2.id(),agent3.id(),agent4.id()])


	    def work1 = agent1.ownsWork(chain[0],chain[1]);
	    logger.info("agent {} owns work {}", agent1,work1)
	    def work2 = agent2.ownsWork(chain[1],chain[2]);
	    logger.info("agent {} owns work {}", agent2,work2)
	    def work3 = agent3.ownsWork(chain[2],chain[3]);
	    logger.info("agent {} owns work {}", agent3,work3)
	    def work4 = agent4.ownsWork(chain[3],chain[0]);
	    logger.info("Created works: {}",[work1.getId(),work2.getId(),work3.getId(),work4.getId()])

	    agent1.knowsAgent(agent2);
	    logger.info("agent {} knows agent {}",agent1,agent2)
	    agent2.knowsAgent(agent3);
	    logger.info("agent {} knows agent {}",agent2,agent3)
	    agent3.knowsAgent(agent4);
	    logger.info("agent {} knows agent {}",agent3,agent4)

	    agentList = [agent1,agent2,agent3,agent4]
	}

}
