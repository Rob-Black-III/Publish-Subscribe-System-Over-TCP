package edu.rit.cs.pubsub;

import java.util.List;

/**
 * Topic class
 */
public class Topic {
	private int id;
	private String name;
	private List<String> keywords;

	/**
	 * Topic Constructor
	 * @param name name of the Topic
	 * @param keywords keywords associated with topic
	 */
	public Topic(String name, List<String> keywords){
		this.id = EventManager.hash(name);
		this.name = name;
		this.keywords = keywords;
	}

	public List<String> getKeywords() {
		return this.keywords;
	}

	public String getName() {
		return this.name;
	}

	public int getId() {
		return this.id;
	}

	@Override
	public String toString(){
		String temp = "Topic [" + this.name + "] with keywords " + this.keywords.toString();
		return temp;
	}
}
