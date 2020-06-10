package edu.rit.cs.pubsub;

import java.util.*;

/**
 * Event Manager is the Server. Dispatches Topics and Events.
 */
public class EventManager{

	//All messages IP

	public static final String BAD_PUB_SUB_INPUT = "Incorrect input. \n" +
			"Usage:	publish TopicName EventTitle all of my content here \n" +
			"		advertise TopicName keyword1,keyword2,keyword3 \n" +
			"		subscribe topic TopicName \n" +
			"		subscribe keywords keyword1,keyword2,keyword3 \n" +
			"		unsubscribe TopicName \n" +
			"		unsubscribe * \n" +
			"		listSubscribedTopics \n" +
			"		listAllTopics \n" +
			"		login UserName \n" +
			"		exit \n" +
            "       Separate keywords by commas, no spaces needed";

	public static final String NO_USER_EXISTS = "No user found for this ip. \n " +
			"Please type 'user <userName>' to make a user or login.";

	// All Clients we have seen
	private List<String> knownClients;

	// Map of usernames to clients (ip) - could have multiple devices, so multiple ips
	private HashMap<String,ArrayList<String>> usernamesToIps;

	// All Topics we have seen
	private List<Topic> topics;

	// All Topics a user is subscribed to
	private HashMap<String, ArrayList<Topic>> subscribedTopicsGivenUsername;

	// All Topics a user is subscribed to
	private HashMap<String, ArrayList<String>> subscribedKeywordsGivenUsername;

	// All IPs mapped to a list of items cached for them
	private HashMap<String, ArrayList<Event>> cachedEventsGivenUsername;

	// The server instance
	private Server serverInstance;

	/**
	 * EventManager is the "server", sending and receiving requests.
	 * @param port port to host server on
	 */
	public EventManager(int port){

		// Make a list to store clients
		this.knownClients = new ArrayList<>();

		// Make a list to store topics
		this.topics = new ArrayList<>();

        // Declare internal data structures.
		//this.getPublishedTopicsGivenIP = new HashMap<>();
		this.usernamesToIps = new HashMap<>();
		this.subscribedTopicsGivenUsername = new HashMap<>();
		this.subscribedKeywordsGivenUsername = new HashMap<>();
		this.cachedEventsGivenUsername = new HashMap<>();

		// Create a server listening on port
		this.serverInstance = new Server(port);
	}

	public String getUsernameFromIP(String ip){
		if(!this.knownClients.contains(ip)){
			return null;
		}

		for(String username : this.usernamesToIps.keySet()){
			if(this.usernamesToIps.get(username).contains(ip)){
				return username;
			}
		}

		return null;
	}

	/**
	 * Parse input, get and send response to thread which forwards to client
	 * @param myInput
	 * @return String - message to send to calling client
	 */
	public String passInput(String myInput){
		//Command passed as "IP", space, usage commands

		String[] splitInput = myInput.split(" ", 5);
		/*
		 * splitInput[0] is the IP from
		 * splitInput[1] is the commandWord
		 * everything after is arguments
		 */

        //Get the ip
        String ip = splitInput[0];

        //Get the command word
        String commandWord = splitInput[1];

		//Get the username for the ip client
		String username = getUsernameFromIP(ip);

		System.out.println("==============================");
		System.out.println("IP:           " + ip);
		System.out.println("Username:     " + username);
		System.out.println("Message:      " + (Arrays.asList(splitInput).subList(1, splitInput.length)).toString());
		//System.out.println("Command Word: " + commandWord);

        //GUID
        if(commandWord.equals("login")){
            if(this.usernamesToIps.containsKey(username)){
            	if(!this.usernamesToIps.get(username).contains(ip)){
					this.usernamesToIps.get(username).add(ip);
				}

				//Forward all cached events to user under username
				String eventsToReturn = "";
				if(this.cachedEventsGivenUsername.get(username).size() > 0){
					System.out.println("Cached Events: ");
					for(Event e : this.cachedEventsGivenUsername.get(username)){
						eventsToReturn = eventsToReturn.concat("\n").concat("     ");
						eventsToReturn = eventsToReturn.concat(e.toString());
					}
				}

                return "Login successful. Welcome back.".concat(eventsToReturn);
            }
            else{
				//Add ip to "known clients"
				if(!this.knownClients.contains(ip)){
					this.knownClients.add(ip);
				}

            	//Add username association with ip (splitInput[2])
				this.usernamesToIps.put(splitInput[2],new ArrayList<>());
                this.usernamesToIps.get(splitInput[2]).add(ip);

                //Instantiate the list of the subscribed topics for the username
                this.subscribedTopicsGivenUsername.put(splitInput[2],new ArrayList<>());
				this.subscribedKeywordsGivenUsername.put(splitInput[2],new ArrayList<>());

                //Instantiate the cached events given username
				this.cachedEventsGivenUsername.put(splitInput[2], new ArrayList<>());

                return "User created.";
            }
        }

        //Parse the actual command from the client
        //Return the result so the handler can forward to client
		//System.out.println("At Switch boi");
		switch(commandWord){
			case "publish":
				String topicName = splitInput[2];
				String eventTitle = splitInput[3];
				String content = splitInput[4];
				System.out.println("publishing event...");

				if(topicName == null || eventTitle == null || content == null){
					return "One or more publish parameters are null.";
				}

				Topic correspondingTopic = getTopicFromName(topicName);
				if(correspondingTopic == null) {
					return "Topic does not exist. Cannot publish event.";
				}

				Event tempEvent = new Event(correspondingTopic,eventTitle,content);

				notifySubscribers(tempEvent);

				System.out.println(tempEvent.toString());

				return "Event Published";
			case "advertise":
				System.out.println("advertising");

				// Get the topic name and keywords and make a new topic
				String[] keywordsArray = splitInput[3].split(",");
				List<String> keywords = new ArrayList<>();
				Collections.addAll(keywords, keywordsArray);
				Topic myNewTopic = new Topic(splitInput[2], keywords);

				// Add topic to the data structures and broadcast to clients
				addTopic(myNewTopic);
                System.out.println(myNewTopic.toString());
				return "Topic Created.";
			case "subscribe":
				System.out.println("subscribing");
				if(splitInput[2].equals("topic")){
					// Check if Topic exists
					// Sub to topic if exists, else say topic does not exist
                    for(Topic t : this.topics){
                        if( (t.getName()).equals(splitInput[3]) ){
                            if((subscribedTopicsGivenUsername.get(username)).contains(t)){
                                String alreadySubscribed = "Error: Already subscribed to topic " + splitInput[3] + ".\n";
                                return alreadySubscribed;
                            }
                            else {
                                addSubscriber( username, t );
                                return "You are subscribed to topic: " + t.getName();
                            }
                        }
                        else {
                        	continue;
                        }
                    }
					String topicNotFound = "Error: Topic " + splitInput[3] + " does not exist.\n";
					return topicNotFound;
				}
				else if(splitInput[2].equals("keywords")){
					// Check if keywords exist in any topic
					// Sub to topic(s), else say topic does not exist.
                    String[] keywordArray = splitInput[3].split(",");
                    List<String> keywordList = new ArrayList<>();
                    Collections.addAll( keywordList, keywordArray );
                    for(String k : keywordList){
                        for(Topic t : this.topics){
                            if( t.getKeywords().contains(k) ){
                                if( !subscribedTopicsGivenUsername.get(username).contains(t) ){
                                    addSubscriber( username, t );
                                }
                            }
                        }
                        //Add keyword to subscriptions
                        this.subscribedKeywordsGivenUsername.get(username).add(k);
                    }

                    return "Subscribed to all topics with keywords: " + splitInput[3];
				}
				else{
					return "Invalid syntax. Must specify 'topic' or 'keywords'. ";
				}
            case "unsubscribe":
                System.out.println("unsubscribing");
                if((subscribedTopicsGivenUsername.get(username).size() == 0)){
                    return "No subscribed topics";
                }
                else if(splitInput[2].equals("*")){ // Unsubscribe from all subscribed topics
                	subscribedTopicsGivenUsername.get(username).clear();
                	subscribedKeywordsGivenUsername.get(username).clear();
                    return "Unsubscribed from all topics";
                }
                else{ // Unsubscribe from a specified topic
                	Topic usTopic = getTopicFromName(splitInput[2]);
                	if(usTopic == null){
                		return "Topic " + splitInput[2] + " does not exist.";
					}
					else if((subscribedTopicsGivenUsername.get(username)).contains(usTopic)){
						removeSubscriber( username, usTopic );
						return "Unsubscribed successfully";

					}
					else if(!((subscribedTopicsGivenUsername.get(username)).contains(usTopic))){
						return "Topic " + splitInput[2] + " not in subscriptions";
					}
					else{
						return "ERROR when unsubscribing";
					}
                }
			case "listAllTopics":
				String temp = "";
				if(topics.size() == 0){
					return "No topics exist.";
				}
				for(Topic t : topics){
					temp = temp.concat(t.toString().concat("    "));
				}
				return temp;
            case "listSubscribedTopics":
                List<Topic> seenTopics = new ArrayList<>();

            	if(splitInput.length != 2){
            		return BAD_PUB_SUB_INPUT;
				}

            	System.out.println("Listing subscribed topics... ");
                // Return a String that has all subscribed topics along with their keywords
				List<Topic> tList = subscribedTopicsGivenUsername.get(username);

				if(tList.isEmpty()){
					return "No subscribed topics to display. ";
				}

                String subscribedTopics = "Subscribed Topics:\n";
                for( Topic t : tList ){
                    seenTopics.add(t);
                    subscribedTopics = subscribedTopics.concat("    ").concat(t.toString());
                }

                return subscribedTopics;
			case "exit":
				//Send response back to client through handler somehow
				System.out.println("Client requests disconnect...");
				return "exit good";
			default:
				return BAD_PUB_SUB_INPUT;
		}
    }

	/**
	 * Queries whether a username is online or not.
	 * @param username username associated with ips
	 * @return boolean true or false
	 */
	public boolean queryUsernameOnline(String username){
		List<String> ipsForUsername = usernamesToIps.get(username);

		if(ipsForUsername == null || ipsForUsername.isEmpty()){
			return false;
		}
		else{
			for(String ip : ipsForUsername){
				if(this.serverInstance.isClientOnline(ip)){
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Returns a topic object from a topic name
	 * @param topicName name of a topic
	 * @return Topic object
	 */
    public Topic getTopicFromName(String topicName){
		for(Topic t : topics){
			if(t.getName().equals(topicName)){
				return t;
			}
		}
		return null;
	}

	/**
	 * Global Hash Function
	 * @param s string to hash
	 * @return hash
	 */
    public static int hash(String s){
		return s.hashCode();
	}

	/**
	 * Starts the listen service
	 */
	private void startService(){
	    this.serverInstance.listen(this);
    }

	/**
	 * Notifies subscribers of a topic/keywords to corresponding events
	 * Supports if person is offline
	 * @param event An Event
	 */
	private void notifySubscribers(Event event) {
		System.out.println("Notifying subscribers of event...");
		System.out.println(event.toString());
        //Forward event to clients who subscribe
        for(String user : this.usernamesToIps.keySet()){
			System.out.println("For user " + user);

			//User is offline
			if(queryUsernameOnline(user) == false){
				(this.cachedEventsGivenUsername.get(user)).add(event);
			}
            if((this.subscribedTopicsGivenUsername.get(user)).contains(event.getTopic())){
				System.out.println("Broadcast to these ips: " + this.usernamesToIps.get(user).toString());
                this.serverInstance.writeToClientGivenIPs(event.toString(), this.usernamesToIps.get(user));
            }
            else{
            	continue;
			}
        }
	}

	/**
	 * Adds a topic to the internal list of topics
	 * @param topic A Topic
	 */
	private void addTopic(Topic topic){
		//Add the topic to the internal list of topics
		this.topics.add(topic);

		//Check keywords and add to existing users
		for(String user : usernamesToIps.keySet()){
			List<String> keywords = this.subscribedKeywordsGivenUsername.get(user);
			for (String keyword : keywords){
				//for each topic, if it has these keywords and not already subscribed, subscribe user
				if(topic.getKeywords().contains(keyword)){
					this.subscribedTopicsGivenUsername.get(user).add(topic);
				}
			}
		}

		//Notify all clients that there is a new topic
		this.serverInstance.writeToAllClients("New Topic: " + topic.getName() + "\n" + "Keywords: " + topic.getKeywords().toString());
	}

	/**
	 * Adds a subscriber to the internal model
	 * @param id username
	 * @param t Topic
	 */
	private void addSubscriber(String id, Topic t){
		if(!((this.subscribedTopicsGivenUsername.get(id)).contains(t)))
			this.subscribedTopicsGivenUsername.get(id).add(t);
	}

	/**
	 * Removes a subscriber from the internal model
	 * @param id username
	 * @param t Topics
	 */
	private void removeSubscriber(String id, Topic t){
		System.out.println("removing subscriber...");
		System.out.println(this.subscribedTopicsGivenUsername.get(id));
		if((this.subscribedTopicsGivenUsername.get(id)).contains(t))
			this.subscribedTopicsGivenUsername.get(id).remove(t);
	}
	
	/**
	 * show the list of subscriber for a specified topic
	 */
	private void showSubscribers(Topic topic){
		for(String user : this.usernamesToIps.keySet()){
			if((subscribedTopicsGivenUsername.get(user)).contains(topic)){
				System.out.println(user);
			}
		}
	}

	/**
	 * Prints program usage and exits
	 */
	private static void printUsageAndExit(){
		System.out.println("Usage: EventManager.java <PORT_NUMBER>");
		System.exit(0);
	}

	/**
	 * Main Driver
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1){
			printUsageAndExit();
		}
		EventManager eventManager = new EventManager(Integer.parseInt(args[0]));

		// All Incoming/Outgoing processing performed in server Handler
		eventManager.startService();
	}
}