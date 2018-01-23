package org.infinispan.creson;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.LinkedList;

@Entity
@Indexed
public class Room{

	private static final long serialVersionUID = 4805204995317415754L;

	@Id
	int id;
	@Field
	int treasure;

	LinkedList<Room>  adjList;

	public Room(){}

	public Room(int id) {
		this.id = id;
		this.treasure = 1;
		this.adjList = new LinkedList<>();
	}

	public int getId(){
		return id;
	}

	public int getTreasure() {
	  return treasure;
	}
	public int loot(){
		if (treasure == 1) {
			treasure = 0;
			return 1;
		}
		return 0;
	}

	public void addRoom(Room room){
		adjList.add(room);
	}

	public LinkedList<Room> getAdjList() {
		return adjList;
	}

	@Override
	public String toString(){
		String ret ="Room"+this.id+"[";
		for(int i=0; i<adjList.size(); i++) {
			ret += adjList.get(i).getId();
			if (i+1!=adjList.size()) ret+=" ";
		}
		ret += "]";
		return ret;
	}

}
