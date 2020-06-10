package edu.rit.cs.pubsub;

public class Event {
	private int id;
	private Topic topic;
	private String title;
	private String content;

	/**
	 * Internal model of an event
	 * @param t Topic the event parents to
	 * @param title Title of the event
	 * @param content Content of the event
	 */
	public Event(Topic t, String title, String content){
		this.id = EventManager.hash(title);
		this.topic = t;
		this.title = title;
		this.content = content;
	}

	public int getHash(){
		return this.id;
	}

	public Topic getTopic(){
		return this.topic;
	}

	public String getTitle(){
		return this.title;
	}

	public String getContent(){
		return this.content;
	}

	@Override
	public String toString(){
		String temp = "Event [" + this.title + "] for [" + this.topic.getName() + "]: " + this.content;
		return temp;
	}
}
