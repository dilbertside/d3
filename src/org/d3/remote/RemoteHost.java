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
package org.d3.remote;

import java.util.concurrent.ConcurrentHashMap;

import org.d3.Console;
import org.d3.HostAddress;
import org.d3.actor.Agency;

public class RemoteHost {
	private final ConcurrentHashMap<String, RemoteAgency> agencies;
	private final ConcurrentHashMap<Integer, RemoteAgency> ports;
	private final HostAddress address;

	public RemoteHost(HostAddress address) {
		this.address = address;
		this.agencies = new ConcurrentHashMap<String, RemoteAgency>();
		this.ports = new ConcurrentHashMap<Integer, RemoteAgency>();
	}

	public HostAddress getAddress() {
		return address;
	}

	public RemoteAgency getRemoteAgency(String id)
			throws UnknownAgencyException {
		RemoteAgency ra = agencies.get(id);

		if (ra == null)
			throw new UnknownAgencyException();

		return ra;
	}

	public RemoteAgency registerAgency(String id) {
		Agency.getLocalAgency().checkBodyThreadAccess();
		RemoteAgency remote = agencies.get(id);

		if (remote == null) {
			remote = new RemoteAgency(this, id);
			agencies.put(id, remote);
			Console.info("register agency \"%s\" @ %s", id, address.getHost());
			Agency.getLocalAgency().getRemoteHosts().getEventDispatcher()
					.trigger(RemoteEvent.REMOTE_AGENCY_REGISTERED, remote);
		}

		return remote;
	}

	public void unregisterAgency(RemoteAgency remote) {
		Agency.getLocalAgency().checkBodyThreadAccess();

		if (agencies.containsKey(remote.getId())) {
			Agency.getLocalAgency().getRemoteHosts().getEventDispatcher()
					.trigger(RemoteEvent.REMOTE_AGENCY_UNREGISTERED, remote);
			agencies.remove(remote.getId());
		}
	}

	public void registerPort(int port, String scheme, RemoteAgency remoteAgency)
			throws RemotePortException, UnknownAgencyException {
		Agency.getLocalAgency().checkBodyThreadAccess();

		if (ports.containsKey(port))
			throw new RemotePortException(String.format("%d in use", port));

		if (!agencies.containsValue(remoteAgency))
			throw new UnknownAgencyException();

		remoteAgency.registerPort(port, scheme);
		ports.put(port, remoteAgency);
	}

	public void unregisterPort(int port) {
		Agency.getLocalAgency().checkBodyThreadAccess();

		RemoteAgency host = ports.get(port);

		if (host != null) {
			host.unregisterPort(port);
			ports.remove(port);
		}
	}

	public String toString() {
		return address.toString();
	}
}
