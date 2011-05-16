/**
 * Copyright (C) 2010 Julien SMADJA <julien dot smadja at gmail dot com> - Arnaud LEMAIRE <alemaire at norad dot fr>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.awired.visuwall.core.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class SoftwareAccess {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String url;
    private String login;
    private String password;
    private String pluginClassName;

    
    // TODO on next version
    // private boolean allProject;
    // private List<ProjectId> projectIds;

    /////////////////////////////////////////////////////////////

//    
//    @ManyToOne
//    @JoinColumn (name="SOFTWARE_ID", nullable = false, updatable = false, insertable = false)
////    @ManyToOne(optional = false)
////    @JoinColumn (name="WALL_ID")
//    private Wall      wall;
    
    /////////////////////////////////////////////////////////////

    public SoftwareAccess() {

    }

    public SoftwareAccess(Class<?> plugin, String url, String name) {
        this.pluginClassName = plugin.getName();
        this.url = url;
        this.name = name;
    }

    public SoftwareAccess(Class<?> plugin, String url, String name, String login, String password) {
        this(plugin, url, name);
        this.login = login;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SoftwareAccess other = (SoftwareAccess) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

	public String getPluginClassName() {
		return pluginClassName;
	}

	public void setPluginClassName(String pluginClassName) {
		this.pluginClassName = pluginClassName;
	}
}