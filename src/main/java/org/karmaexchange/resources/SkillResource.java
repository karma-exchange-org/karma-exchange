package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.Skill;

@Path("/skill")
public class SkillResource extends BaseNamedResource<Skill> {

  @Override
  protected Class<Skill> getResourceClass() {
    return Skill.class;
  }

  @Override
  protected String getResourceName(Skill skill) {
    return skill.getId();
  }
}
