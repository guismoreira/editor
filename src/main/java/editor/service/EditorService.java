package editor.service;


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

import editor.model.Activity;
import editor.model.Code;
import editor.model.Group;
import editor.model.Status;
import editor.model.Status.StatusEnum;
import editor.model.User;

@RequestScoped
@Path("/api/v1/")
public class EditorService extends BaseService implements EditorInterface {

    @POST
    @Path("createUser")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public User createUser(@FormParam("name") final String name, @FormParam("hashUser") final String hashUser) throws WebApplicationException{
        
        final User hashCheck = userRepository.find(QUERY_HASH_USER, hashUser).firstResult();

        if(name.isEmpty() || hashCheck != null) {
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            final User user = new User(hashUser, name);
            userRepository.persist(user);

        return user;
    }

    @POST
    @Path("createGroup")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Group createGroup(
        @FormParam("namegroup") final String namegroup,
        @FormParam("hashUser") final String hashUser
    )throws WebApplicationException{

        final User user = userRepository.find(QUERY_HASH_USER, hashUser).firstResult();
        final Group groupCheck = groupRepository.find(QUERY_NAME, namegroup).firstResult();

        if(namegroup.isEmpty() || user == null || groupCheck != null) {
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            final Group group = new Group(List.of(user), namegroup);
            groupRepository.persist(group);

        return group;

    }

    @PUT
    @Path("joingroup")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Group joinGroup(
        @FormParam("hashUser") final String hashUser, 
        @FormParam("namegroup") final String namegroup) throws WebApplicationException {

        final User user = userRepository.find(QUERY_HASH_USER, hashUser).firstResult();
        final Status status = new Status();
        final Group group = groupRepository.find(QUERY_NAME, namegroup).firstResult();

        if(user==null || group==null) {
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            status.setStatusEnum(StatusEnum.BLOCKED);
            user.addStatuses(status);
            group.addUser(user);
            groupRepository.persist(group);

        return group;
    }

    @POST
    @Path("createActivity")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Activity createActivity(
        @FormParam("namegroup") final String namegroup) throws WebApplicationException {
        final Activity activity = new Activity();
        final Group group = groupRepository.find(QUERY_NAME, namegroup).firstResult();
        final Status status = new Status();
        
        
        if(group==null){
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            createCode();
            status.setStatusEnum(StatusEnum.ACTIVE);
            group.addStatus(status);
           
            activity.set_group(group);
            activityRepository.persist(activity);

        return activity;

    }

    @GET
    @Path("/checkStatus")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public Status checkStatus(@FormParam("namegroup") String namegroup) throws WebApplicationException{
        
        final Group group = groupRepository.find(QUERY_NAME, namegroup).firstResult();
        final Status status = statusRepository.find("_group_id", group.getId()).firstResult();

        if(group==null || status==null){
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            group.getId();
            status.getStatusEnum();

        return status;

    }

    @PUT
    @Path("/participates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Override
    public String participates(
        @FormParam("hashUser") final String hashUser, 
        @FormParam("namegroup") final String namegroup) throws WebApplicationException{

        final User user = userRepository.find(QUERY_HASH_USER, hashUser).firstResult();
     
        final Group group = groupRepository.find(QUERY_NAME, namegroup).firstResult();
        final Activity activity = activityRepository.find("_group_id", group.getId()).firstResult();
        final Code checkUser = codeRepository.find("user_id", user.getId()).firstResult();
        Code code = codeRepository.find("order by id desc").firstResult();
        
        if(group==null || user==null || checkUser!=null || activity==null){
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            code.setUser(user);
            activity.setUser(user);
            code.setActivity(activity);
            codeRepository.isPersistent(code);

            return URL_BLOCKLY + code.getHashCode() + "&lblock=" + code.getLimitBlock();
    }

    @POST
    @Path("/listActivities")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Override
    public List<Activity> listActivities(@FormParam("hashUser") String hashUser)throws WebApplicationException{
        final User user = userRepository.find(QUERY_HASH_USER, hashUser).firstResult();
        final List<Activity> activity = activityRepository.list("user_id", user.getId());

        if(activity==null) {
            throw new ServiceException(EMPTY_DATA, Response.Status.BAD_REQUEST);
        }
            return activity;
    }
   

    @POST
    @Path("createCode")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code createCode() throws WebApplicationException {
        Code code = new Code();

            Code codeSave = new Code(null, null, LIMIT_BLOCK);
            codeRepository.persist(code);

        return code;

    }

    @POST
    @Path("incrementCode/{hash}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code incrementCode(@PathParam("hash") final String hash, @FormParam("textCode") final String textCode) throws WebApplicationException {

        Code code = new Code();
        Code lastcode = codeRepository.find(QUERY_HASH_CODE, hash).firstResult();
        try {
                code.setTextCode(textCode);
                String newHash = code.setHashCode(code.generateHash());
                int limitBlock = lastcode.getLimitBlock() + LIMIT_BLOCK;
                code.setHashCode(newHash);
                code.setLimitBlock(limitBlock);
                codeRepository.persist(code);

        } catch (Exception e) {
            throw new ServiceException(CODE_NOT_FOUND, Response.Status.NOT_FOUND);
        }
        return code;

    }

    @GET
    @Path("/loadCode/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Code loadCode(@PathParam("hash") final String hash) throws WebApplicationException {
        Code code = new Code();
        try {
            code = codeRepository.find(QUERY_HASH_CODE, hash).firstResult();
        } catch (Exception e) {
            throw new ServiceException(CODE_NOT_FOUND, Response.Status.NOT_FOUND);
        }
        return code;
    }
}
