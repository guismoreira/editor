package editor.controller;


/**
 * Copyright 2021 Blockly Service @ https://github.com/orion-services/blockly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

import editor.entity.Activity;
import editor.entity.Code;
import editor.entity.Group;
import editor.entity.Status;
import editor.entity.Status.StatusEnum;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import editor.entity.User;

@RequestScoped
@Path("/api/v1/")
public class EditorController extends BaseController implements EditorInterface {

    EntityManager em;

    @POST
    @Path("createUser")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public User createUser(@FormParam("name") final String name, @FormParam("hashUser") final String hashUser) throws WebApplicationException{
        final User user = new User();
        final User hashCheck = userDAO.find("hashUser", hashUser).firstResult();

        if(name.isEmpty()) {
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("name cannot be empty");
            Response response = builder.build();
            throw new WebApplicationException(response);

        }else if(hashCheck != null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("already registered user");
            Response response = builder.build();
            throw new WebApplicationException(response);

        }else{
            user.setHashUser(hashUser);
            user.setName(name);
            userDAO.persist(user);
        }
        return user;
    }

    @POST
    @Path("createGroup")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Group createGroup(
        @FormParam("namegroup") final String namegroup,
        @FormParam("hashUser") final String hashUser
    )throws WebApplicationException{
        final Group group = new Group();
        final User user = userDAO.find("hashUser", hashUser).firstResult();
        final Group groupCheck = groupDAO.find("name", namegroup).firstResult();

        if(namegroup.isEmpty() || user == null) {
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("user is null");
            Response response = builder.build();
            throw new WebApplicationException(response);

        }else if(groupCheck != null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("already registered group");
            Response response = builder.build();
            throw new WebApplicationException(response);

        }else{
            group.addUser(user);
            group.setName(namegroup);
            groupDAO.persist(group);

        }

        return group;

    }

    @PUT
    @Path("joingroup")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Group joinGroup(
        @FormParam("hashUser") final String hashUser, 
        @FormParam("namegroup") final String namegroup) throws WebApplicationException {

        final User user = userDAO.find("hashUser", hashUser).firstResult();
        final Status status = new Status();
        final Group group = groupDAO.find("name", namegroup).firstResult();

        if(user==null || group==null) {
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("empty data, make sure you fill in correctly and try again");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }else{
            status.setStatusEnum(StatusEnum.BLOCKED);
            user.addStatuses(status);
            group.addUser(user);
            groupDAO.persist(group);
        }
        return group;
    }

    @POST
    @Path("createActivity")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Activity createActivity(
        @FormParam("namegroup") final String namegroup) throws WebApplicationException {
        final Activity activity = new Activity();
        final Group group = groupDAO.find("name", namegroup).firstResult();
        final Status status = new Status();
        Code code = new Code();
        
        if(group==null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("empty data, make sure you fill in correctly and try again");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }else{
            code = createCode();
            status.setStatusEnum(StatusEnum.ACTIVE);
            group.addStatus(status);
            activity.addCode(code);
            activity.setUgroup(group);
            activityDAO.persist(activity);
        }
        return activity;

    }

        /**
     * Returns the current locks editor Activity object of a group
     * 
     * @param alias : An unique name of the group
     * @return The blocks editor Activity object 
     */
    @GET
    @Path("/checkStatus")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Status checkStatus(@FormParam("namegroup") String namegroup) throws WebApplicationException{
        
        final Group group = groupDAO.find("name", namegroup).firstResult();
        final Status status = statusDAO.find("ugroup_id", group.getId()).firstResult();

        if(group==null || status==null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("empty data, make sure you fill in correctly and try again");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }else{
            group.getId();
            status.getStatusEnum();
        }

        return status;

    }
    

    /**
     * Asks to participates in a group activity
     * 
     * @param alias : An unique name of the group
     * @return Return a URL to participates of a activity 
     */
    @PUT
    @Path("/participates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Override
    public String participates(
        @FormParam("hashUser") final String hashUser, 
        @FormParam("namegroup") final String namegroup) throws WebApplicationException{

        final User user = userDAO.find("hashUser", hashUser).firstResult();
     
        final Group group = groupDAO.find("name", namegroup).firstResult();
        final Activity activity = activityDAO.find("ugroup_id", group.getId()).firstResult();
        final Code code = codeDAO.find("activity_id", activity.getId()).firstResult();

        if(group==null || user==null || code==null || activity==null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("empty data, make sure you fill in correctly and try again");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }else{

          code.setUser(user);
          activity.setUser(user);
          codeDAO.isPersistent(code);

            return "http://0.0.0.0:7000/?hash=" + code.getHashCode() + "&lblock=" + code.getLimitBlock();
        } 
    }

    /**
     * Lists all current activities of an user
     * 
     * @param discriminator : The Discord discriminator 
     * @return Return a list of current activity of a user in all groups
     */
    @POST
    @Path("/listActivities")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public List<Activity> listActivities(@FormParam("hashUser") String hashUser)throws WebApplicationException{
        final User user = userDAO.find("hashUser", hashUser).firstResult();
        final List<Activity> activity = activityDAO.list("user_id", user.getId());

        if(activity==null){
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.BAD_REQUEST);
            builder.entity("empty data, make sure you fill in correctly and try again");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }else{
            return activity;
        }

    }
   

    @POST
    @Path("createCode")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code createCode() throws WebApplicationException {
        Code code = new Code();
        code.setTextCode("");

            String hash = code.setHashCode(code.generateHash());
            code.setHashCode(hash);
            code.setLimitBlock(5);
            codeDAO.persist(code);

        return code;

    }

    @POST
    @Path("incrementCode/{hash}")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code incrementCode(@PathParam("hash") final String hash, @FormParam("textCode") final String textCode) throws WebApplicationException {

        Code code = new Code();
        Code lastcode = codeDAO.find("hashCode", hash).firstResult();
        try {

            code.setTextCode(textCode);
                String newHash = code.setHashCode(code.generateHash());
                long limitBlock = lastcode.getLimitBlock() + 5;
                code.setHashCode(newHash);
                code.setLimitBlock(limitBlock);
                codeDAO.persist(code);

        } catch (Exception e) {
            throw new WebApplicationException("Code not found", Response.Status.NOT_FOUND);
        }
        return code;

    }

    @GET
    @Path("/loadCode/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code loadCode(@PathParam("hashCode") final String hashCode) throws WebApplicationException {
        Code code = new Code();
        try {
            code = codeDAO.find("hashCode", hashCode).firstResult();
        } catch (Exception e) {
            throw new WebApplicationException("Code not found", Response.Status.NOT_FOUND);
        }
        return code;
    }

  


   //1 - um usuario pode estar em um grupo, um grupo pode ter N usuarios
   //2 - um grupo pode fazer N atividades, uma atividade pode ser feita por um grupo
   //3 - pegar id da atividade e anexar na URL 
   

}
