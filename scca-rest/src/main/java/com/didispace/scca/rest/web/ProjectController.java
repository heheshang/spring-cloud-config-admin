package com.didispace.scca.rest.web;

import com.didispace.scca.core.domain.Env;
import com.didispace.scca.core.domain.Label;
import com.didispace.scca.core.domain.Project;
import com.didispace.scca.rest.dto.EnvDto;
import com.didispace.scca.rest.dto.LabelDto;
import com.didispace.scca.rest.dto.ProjectDto;
import com.didispace.scca.rest.dto.base.WebResp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by 程序猿DD/翟永超 on 2018/4/27.
 * <p>
 * Blog: http://blog.didispace.com/
 * Github: https://github.com/dyc87112/
 */
@Api("Project MGT（项目管理）")
@Slf4j
@RestController
@RequestMapping("${scca.rest.context-path:}/project")
public class ProjectController extends BaseController {

    @ApiOperation("List Project / 项目列表")
    @RequestMapping(method = RequestMethod.GET)
    public WebResp<List<ProjectDto>> findAll() {
//        Pageable pageable = new PageRequest(page, size);
        List<ProjectDto> result = new ArrayList<>();
        for (Project project : projectRepo.findAll()) {
            ProjectDto dto = new ProjectDto();
            BeanUtils.copyProperties(project, dto, "envs", "labels");

            for (Env env : project.getEnvs()) {
                EnvDto envDto = new EnvDto();
                BeanUtils.copyProperties(env, envDto);
                dto.getEnvs().add(envDto);
            }

            for (Label label : project.getLabels()) {
                LabelDto labelDto = new LabelDto();
                BeanUtils.copyProperties(label, labelDto);
                dto.getLabels().add(labelDto);
            }

            result.add(dto);
        }
        return WebResp.success(result);
    }

    @ApiOperation("Project Detail / 项目详情")
    @RequestMapping(path = "/detail", method = RequestMethod.GET)
    public WebResp<ProjectDto> findProjectDetail(@RequestParam("id") Long id) {

        Project project = projectRepo.findOne(id);
        ProjectDto dto = new ProjectDto();
        BeanUtils.copyProperties(project, dto);

        return WebResp.success(dto);
    }

    @Transactional
    @ApiOperation("Create Project / 创建项目")
    @RequestMapping(method = RequestMethod.POST)
    public WebResp<String> create(@RequestBody ProjectDto project) {
        log.info("create Project : " + project);

        Project saveProject = new Project();
        BeanUtils.copyProperties(project, saveProject, "envs", "labels");
        // 关联环境（env)
        for (EnvDto envDto : project.getEnvs()) {
            Env env = envRepo.findOne(envDto.getId());
            saveProject.getEnvs().add(env);
        }
        saveProject = projectRepo.save(saveProject);

        // 关联默认版本（label）
        for (LabelDto labelDto : project.getLabels()) {
            if(Objects.equals(labelDto.getId(), null)){
                Label label = new Label();
                label.setName(labelDto.getName());
                label.setProject(saveProject);
                labelRepo.save(label);
            }
        }

        return WebResp.success("create Project success");
    }

    @Transactional
    @ApiOperation("Delete Project / 删除项目")
    @RequestMapping(method = RequestMethod.DELETE)
    public WebResp<String> deleteProject(@RequestParam("id") Long id) {
        Project project = projectRepo.findOne(id);
        Assert.notNull(project, "Project [" + id + "] not exist");

        log.info("delete Project : " + project.getName());

        // 级联删除配置版本的数据与实际配置存储
        persistenceService.deletePropertiesByProject(project);

        // 删除scca中的管理实体
        projectRepo.delete(project);

        return WebResp.success("delete Project success");
    }

    @Transactional
    @ApiOperation("Update Project / 更新项目")
    @RequestMapping(method = RequestMethod.PUT)
    public WebResp<String> updateProject(@RequestBody ProjectDto project) {
        Project updateProject = projectRepo.findOne(project.getId());
        Assert.notNull(updateProject, "Project [" + project.getId() + "] not exist");

        log.info("update Project : " + updateProject + " --> " + project);

        // TODO 增加部署环境与配置版本的更新，移除的需要联合删除操作
        List<Env> envs = updateProject.getEnvs();


        // TODO 整理出要新增或删除的Env


        updateProject.setName(project.getName());
        projectRepo.save(updateProject);

        return WebResp.success("update Project success");
    }

    @Transactional
    @ApiOperation("Project Add Label / 项目增加配置版本")
    @RequestMapping(path = "/label", method = RequestMethod.POST)
    public WebResp<String> addProjectLabel(@RequestParam("projectId") Long projectId,
                                           @RequestParam("labelName") String labelName) {
        Project owner = projectRepo.findOne(projectId);
        Assert.notNull(owner, "Project [" + projectId + "] not exist");

        Label label = new Label();
        label.setName(labelName);
        label.setProject(owner);
        labelRepo.save(label);

        return WebResp.success("create project [" + owner.getName() + "] label [" + labelName + "] success");
    }


    @Transactional
    @ApiOperation("Project Delete Label / 项目删除版本")
    @RequestMapping(path = "/label", method = RequestMethod.DELETE)
    public WebResp<String> deleteProjectLabel(@RequestParam("labelId") Long labelId) {
        Label label = labelRepo.findOne(labelId);
        Assert.notNull(label, "Label [" + labelId + "] not exist");

        log.info("delete Label [{}-{}]", label.getProject().getName(), label.getName());

        // 同步删除配置的存储
        persistenceService.deletePropertiesByLabel(label);

        // 删除scca中的管理实体
        labelRepo.delete(label);

        return WebResp.success("delete project [" + label.getProject().getName() + "] label [" + label.getName() + "] success");
    }

}
