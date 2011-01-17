/*
 * This file is part of d3.
 * 
 * d3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * d3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with d3.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2010 Guilhelm Savin
 */
package org.d3.test;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.d3.Console;
import org.d3.Actor;
import org.d3.actor.Agency;
import org.d3.actor.Entity;
import org.d3.annotation.ActorPath;
import org.d3.annotation.Callable;

import static org.d3.Actor.Tools.call;

@ActorPath("/d3/test/entities")
public class TestEntity extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4661324109912888393L;

	static Random random = new Random();

	ConcurrentLinkedQueue<Actor> idObjects;

	public TestEntity(String entityId) {
		super(entityId);
		idObjects = new ConcurrentLinkedQueue<Actor>();
	}

	@Callable("beMyFriend")
	public void addFriend(URI uri) {
		Actor idObject = Agency.getLocalAgency().getIdentifiableObject(uri);

		if (idObject != null)
			idObjects.add(idObject);
	}
	
	@Callable("ping")
	public void ping() {
		//Console.info("[%s] ping",getId());
	}

	@Callable("pong")
	public void pong() {
		//Console.info("[%s] pong",getId());
	}

	@Callable("step")
	public void step() {
		//Console.warning("entity step (friends: %d)",idObjects.size());
		
		int i = random.nextInt(idObjects.size());
		
		for (Actor idObject : idObjects) {
			if (i-- == 0) {
				String action;
				
				if(random.nextBoolean())
					action = "pong";
				else
					action = "ping";
				
				call(this,idObject,action,null,false);
			}
		}
	}
}