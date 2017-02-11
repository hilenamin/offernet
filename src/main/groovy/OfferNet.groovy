//@Grab(group='com.datastax.cassandra', module='dse-driver', version='1.1.1')
//@Grab(group='log4j', module='log4j', version='1.2.17')

package net.vveitas.offernet

import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;

import com.datastax.driver.dse.graph.GraphStatement;
import com.datastax.driver.dse.graph.SimpleGraphStatement;
import com.datastax.driver.dse.graph.GraphResultSet
import com.datastax.driver.dse.graph.GraphOptions

import com.datastax.driver.dse.graph.Edge
import com.datastax.driver.dse.graph.Vertex

import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class OfferNet implements AutoCloseable {

    private DseCluster cluster;
    private DseSession session; // creating one 'main' client and allowing to create more with the method
    private Logger logger;

    public void ass() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          sesion.close();
          cluster.close();
        }
      });
    }

    private OfferNet() {

        //loading log4j properties
        def config = new ConfigSlurper().parse(new File('configs/log4j-properties.groovy').toURL())
        PropertyConfigurator.configure(config.toProperties())
        logger = LoggerFactory.getLogger('OfferNet.class');

        try {
            def start = System.currentTimeMillis()
            cluster = DseCluster.builder().addContactPoint("192.168.1.6").build();
            cluster.connect().executeGraph("system.graph('offernet').ifNotExists().create()");

            cluster = DseCluster.builder()
                .addContactPoint("192.168.1.6")
                .withGraphOptions(new GraphOptions().setGraphName("offernet"))
                .build();
            session = cluster.connect();

            logger.info("Created OfferNet instance with session {}", session);
            logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        String clusterName = cluster.toString()
        String sessionName = session.toString()
        this.session.close();
        logger.info("Closed session {}",sessionName);
        this.cluster.close();
        logger.info("Closed cluster {}",clusterName);
    }

    public List createAgentNetwork(int numberOfAgents) {
        def start = System.currentTimeMillis()
        List agentsList = new ArrayList()
        agentsList.add(new Agent(this.session))

        while (agentsList.size() < numberOfAgents) {
            def random = new Random();
            def i = random.nextInt(agentsList.size())
            Object Agent1 = agentsList[i]
            Object Agent2 = new Agent(this.session)
            Agent1.knowsAgent(Agent2)
            agentsList.add(Agent2)
        }
        logger.info("Created a network of "+numberOfAgents+ " Agents")
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)        
        return agentsList;
    }

    public void flushVertices(String labelName) {
      Map params = new HashMap();
      params.put("labelName", labelName);

      SimpleGraphStatement s = new SimpleGraphStatement("g.V().has(label,labelName).drop()",params);
      GraphResultSet rs = session.executeGraph(s);
      logger.warn("Executed statement: {}", Utils.getStatement(rs));
      logger.warn("Execution warnings from the server: {}", Utils.getWarnings(rs));
      logger.info("Dropped vertexes with label {} from OfferNet", labelName);
    }

    public Object flushVertices() {
      SimpleGraphStatement s = new SimpleGraphStatement("g.V().drop()");
      GraphResultSet rs = session.executeGraph(s);
      logger.warn("Executed statement: {}", Utils.getStatement(rs));
      logger.warn("Execution warnings from the server: {}", Utils.getWarnings(rs));
      logger.info("Dropped all vertexes from OfferNet");
      return this;
    }

    public List getIds(String labelName) {
      def start = System.currentTimeMillis()
      Map params = new HashMap();
      params.put("labelName", labelName);

      SimpleGraphStatement s = new SimpleGraphStatement("g.V().has(label,labelName).id()",params);
      GraphResultSet rs = session.executeGraph(s);
      logger.warn("Executed statement: {}", Utils.getStatement(rs));
      logger.warn("Execution warnings from the server: {}", Utils.getWarnings(rs));

      List<Object> agentIds = rs.all();
      logger.info("Retrieved list of {} agentIds from OfferNet", agentIds.size());
      logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)
      return agentIds;
    }

    public addRandomWorksToAgents(int numberOfWorks) {
        def start=System.currentTimeMillis();
        List agentIds = this.getIds('agent');
        numberOfWorks.times {
            def random = new Random();
            def i = random.nextInt(agentIds.size())
            def agent = new Agent(agentIds[i],this.session);
            def ownsWork = agent.ownsWork();
            logger.info("Added ownsWork link {} to agent {}", ownsWork, agent.id());
        }
        logger.info("Added "+numberOfWorks+" of random processes to the network")
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)
    }

    private List addChainToNetwork(List chain) {
        def start = System.currentTimeMillis();
        def dataItemsWithDesignedSimilarities = new ArrayList()
        def chainedWorks = []
        for (int x=0;x<chain.size()-1;x++) {
            def random = new Random();
            def agentIds = this.getIds('agent');
            def i = random.nextInt(agentIds.size())
            def agent = new Agent(agentIds[i],this.session)
            def work = new Work(this.session);
            chainedWorks.add(work.id())
            def demand = work.addDemand(new Item(chain[x],this.session))
            def offer = work.addOffer(new Item(chain[x+1],this.session))
            agent.ownsWork(work);
            //print a list with items which have designed similarities in the network
            switch (x) {
                case 0:
                    // for the first item in chain we add only offer
                    logger.info("First item with designed similarity {}:{}", offer,offer.getValue())
                    break
                case chain.size()-2:
                    //for the last item in chain we add only demand
                    logger.info("Last item with designed similarity {}:{}", demand,demand.getValue())
                    break
                default:
                    //otherwise we add both, because it has similarity both ways
                    logger.info("Item with designed similarity {}:{}", demand,demand.getValue())
                    logger.info("Item with designed similarity {}:{}", offer,offer.getValue())
                    break
            }
        }
        logger.info("Added a chain to the network {}",chainedWorks)
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)
        return chainedWorks;
    }

    /* 
    * Note that this function is for testing only - it calculates the perfect similarities between items
    */ 
    public List allConnectedSimilarPairsCentralized(Integer similarityThreshold) {
        logger.warn("Centralized search of all demand-offer pairs with perfect similarities in the network");
        def start = System.currentTimeMillis();
        Map params = new HashMap();
        params.put("similarityThreshold", similarityThreshold);


        SimpleGraphStatement s = new SimpleGraphStatement(
                "g.V().match("+
                "__.as('g').has(label,'work').as('w').out('offers').as('o').properties('value').value().as('b')"+
                ",__.as('o').outE('similarity').as('s').properties('similarity').value().is(gte(similarityThreshold))"+
                ",__.as('s').inV().as('d')"+
                ",__.as('d').properties('value').value().as('b')"+
                ",__.as('d').in('demands').as('w2')"+
                ").select('b','o','d')",params);

        GraphResultSet rs = session.executeGraph(s);
        List pairs = rs.all();
        logger.info("Found {} demand-offer pairs existing in the network", pairs.size());
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

        return pairs;

    }

    public List allPathsCentralized(Integer similarityThreshold) {
        logger.warn("Centralized search of all paths in the network");
        def start = System.currentTimeMillis();
        Map params = new HashMap();
        params.put("similarityThreshold", similarityThreshold);

        SimpleGraphStatement s = new SimpleGraphStatement(
                "g.V().has(label,'work').as('source')"+
                ".until(outE('demands').inV().has(label,'item').bothE('similarity').has('similarity',gte(similarityThreshold)).count().is(0))"+
                ".repeat(__.outE('demands').inV().as('a').has(label,'item')"+
                  ".bothE('similarity').has('similarity',gte(similarityThreshold)).bothV().as('b').where('a',neq('b'))"+
                  ".inE('offers').outV().has(label,'work')).simplePath().path()"+
                  ".where(count(local).is(neq(1)))",params);

        GraphResultSet rs = session.executeGraph(s);
        List paths = rs.all().collect{it.asPath().getObjects()}
        logger.warn("Found {} paths",paths);
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

        return paths
    }

    // this query mysteriously does not work -- looks like something is wrong with the type of 'v', as sometimes it works and sometimes not.

    public List allSimilarPairsCentralized() {
        logger.warn("Centralized search of connected demand-offer pairs with perfect similarities in the network");
        def start = System.currentTimeMillis();

        SimpleGraphStatement s = new SimpleGraphStatement(
                "g.V().match("+
                "__.as('g').has(label,'work').out('offers').as('o').properties('value').value().as('v')"+
                ",__.as('g').has(label,'work').out('demands').as('d').properties('value').value().as('v')"+
                ").select('o','d','v')");

        GraphResultSet rs = session.executeGraph(s);
        List pairs = rs.all();
        logger.info("Found {} demand-offer pairs existing in the network", pairs.size());
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

        return pairs;
    }

    public List allWorkItemEdges(String itemName) {
        logger.warn("Centralized search of work vertexes in the graph");
        def start = System.currentTimeMillis();

        Map params = new HashMap();
        params.put("labelName", "work");
        params.put("itemName",itemName);
        params.put("propertyName","value");

        SimpleGraphStatement s = new SimpleGraphStatement(
              "g.V().has(label,labelName).outE(itemName).as('d').inV().properties(propertyName).as('v').select('d','v')"
              ,params)

        GraphResultSet rs = session.executeGraph(s);
        List edges = rs.all();
        logger.info("Found {} allWorkItemEdges of {} existing in the network", edges.size(), itemName);
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)        

        return edges

    }

    public List allSimilarityEdges() {

        def start = System.currentTimeMillis();
        Map params = new HashMap();
        params.put("labelName", "similarity");

        logger.warn("Returning all similarity links");

        SimpleGraphStatement s = new SimpleGraphStatement(
                "g.E().has(label,labelName)",params);

        GraphResultSet rs = session.executeGraph(s);
        List edges = rs.all();
        logger.info("Found {} similarity Edges existing in the network", edges.size());
        logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

        return edges;
    }

    public void removeEdges(String edgeLabel) {
        Map params = new HashMap();
        params.put("labelName", edgeLabel);

        logger.warn("Removing all similarity links");

        SimpleGraphStatement s = new SimpleGraphStatement(
                "g.E().has(label,labelName).drop()",params);

        GraphResultSet rs = session.executeGraph(s);
        logger.info("Removed all {} edges from the graph", edgeLabel);
    }


    public Integer connectMatchingPairs(Map matchingPairs) {
      def start = System.currentTimeMillis();
      def connected = 0;
      logger.info("Connecting all matching demand offer item pairs")
      matchingPairs.each { key,value ->
         def offers = value.get('offers');
         offers.each {offerEdge -> 
            offerEdge = offerEdge.asEdge()
            logger.info("Offer edge: {}", offerEdge)
            def item1 = offerEdge.getInV();
            String item1Label = "item:"+item1.community_id+":"+item1.member_id;
            logger.info("Offer item: {}", item1)
            def demands = value.get('demands');
            demands.each{demandEdge -> 
              demandEdge = demandEdge.asEdge()
              def item2 = demandEdge.getInV();
              String item2Label = "item:"+item2.community_id+":"+item2.member_id;
              logger.info("Demand item: {}", item2);
              def similarity = Utils.calculateSimilarity(key.asString(),key.asString())
              this.connectItems(item1Label,item2Label,similarity)
              connected += 1
            }
         }
      }
      logger.info("Connected {} item pairs.", connected)
      logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)
      return connected
    }

    public Edge connectItems(String item1Label, String item2Label, Integer similarity) {
      def start = System.currentTimeMillis();
      Map params = new HashMap();
      params.put("item1", item1Label);
      params.put("item2",item2Label);
      params.put('edgeLabel','similarity');
      params.put('valueKey','similarity');
      params.put('valueName',similarity);

      logger.warn("Creating similarity edge from item {} to item {} with value {}", params.item1, params.item2, similarity)

      SimpleGraphStatement s = new SimpleGraphStatement(
              "def v1 = g.V(item1).next()\n" +
              "def v2 = g.V(item2).next()\n" +
              "def e = v1.addEdge(edgeLabel, v2)\n"+
              "e.property(valueKey,valueName)\n"+
              "e", params);

      GraphResultSet rs = session.executeGraph(s);
      def similarityEdge = rs.one().asEdge();
      logger.info("Added similarity edge {} to the network", similarityEdge);
      logger.warn("Method {} took {} seconds to complete", Utils.getCurrentMethodName(), (System.currentTimeMillis()-start)/1000)

      return similarityEdge;
    }

}
