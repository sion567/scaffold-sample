package com.scaffold.gen.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * 模板渲染冒烟测试（JPA 范式）：渲染全部在册模板，断言生成物为
 * {@code @Entity + ScaffoldRepository + JpaSpecs} 形态，且不再产出 mapper。
 *
 * @author ct
 */
class VelocityJpaRenderTest
{
    @BeforeAll
    static void initVelocity()
    {
        VelocityInitializer.initVelocity();
    }

    /** 组装一张 CRUD 演示表（主键 ID 自增；LIKE/EQ/BETWEEN 查询列各一） */
    private GenTable demoTable()
    {
        GenTable table = new GenTable();
        table.setTableName("demo_user");
        table.setTableComment("演示用户");
        table.setClassName("DemoUser");
        table.setPackageName("com.demo");
        table.setModuleName("demo");
        table.setBusinessName("user");
        table.setFunctionName("演示用户");
        table.setFunctionAuthor("ct");
        table.setTplCategory("crud");
        table.setFormColNum(2);

        GenTableColumn id = new GenTableColumn();
        id.setColumnName("user_id");
        id.setColumnComment("用户ID");
        id.setColumnType("bigint(20)");
        id.setJavaType("Long");
        id.setJavaField("userId");
        id.setIsPk("1");
        id.setIsIncrement("1");
        id.setIsInsert("1");
        id.setSort(1);

        GenTableColumn name = new GenTableColumn();
        name.setColumnName("user_name");
        name.setColumnComment("用户名");
        name.setColumnType("varchar(50)");
        name.setJavaType("String");
        name.setJavaField("userName");
        name.setIsQuery("1");
        name.setQueryType("LIKE");
        name.setIsList("1");
        name.setIsInsert("1");
        name.setIsEdit("1");
        name.setSort(2);

        GenTableColumn status = new GenTableColumn();
        status.setColumnName("status");
        status.setColumnComment("状态");
        status.setColumnType("char(1)");
        status.setJavaType("String");
        status.setJavaField("status");
        status.setIsQuery("1");
        status.setQueryType("EQ");
        status.setIsList("1");
        status.setIsInsert("1");
        status.setIsEdit("1");
        status.setSort(3);

        table.setColumns(List.of(id, name, status));
        // 真实生成流程由 GenTableServiceImpl#setPkColumn 回填主键列，模板依赖它
        table.setPkColumn(id);
        return table;
    }

    private String render(GenTable table, String templatePath)
    {
        VelocityContext context = VelocityUtils.prepareContext(table);
        Template tpl = Velocity.getTemplate(templatePath, "UTF-8");
        StringWriter sw = new StringWriter();
        tpl.merge(context, sw);
        return sw.toString();
    }

    @Test
    @DisplayName("domain 模板产出 JPA 实体（@Entity/@Table/@Id IDENTITY，无 params 重复字段）")
    void domainTemplate_rendersJpaEntity()
    {
        String code = render(demoTable(), "vm/java/domain.java.vm");
        assertTrue(code.contains("@Entity"));
        assertTrue(code.contains("@Table(name = \"demo_user\")"));
        assertTrue(code.contains("@Id"));
        assertTrue(code.contains("@GeneratedValue(strategy = GenerationType.IDENTITY)"));
        assertTrue(code.contains("extends BaseEntity"));
        // BaseEntity 已有 params，实体不得重复声明
        assertFalse(code.contains("private Map<String, Object> params;"));
        assertFalse(code.contains("Mapper"));
    }

    @Test
    @DisplayName("repository 模板产出 ScaffoldRepository 子接口")
    void repositoryTemplate_rendersRepository()
    {
        String code = render(demoTable(), "vm/java/repository.java.vm");
        assertTrue(code.contains("interface DemoUserRepository extends ScaffoldRepository<DemoUser, Long>"),
                () -> "实际渲染：" + code);
    }

    @Test
    @DisplayName("serviceImpl 模板按查询列生成 JpaSpecs 条件，无 mapper 依赖")
    void serviceImplTemplate_rendersSpecs()
    {
        String code = render(demoTable(), "vm/java/serviceImpl.java.vm");
        assertTrue(code.contains("JpaSpecs.likeIf(\"userName\""));
        assertTrue(code.contains("JpaSpecs.eqIfNotBlank(\"status\""));
        assertTrue(code.contains("TableDataInfo.from(demoUserRepository.page(toSpec(demoUser)"));
        assertTrue(code.contains("selectDemoUserPage"));
        assertFalse(code.contains("Mapper"));
    }

    @Test
    @DisplayName("resourceImpl 分页走 service 的 selectXxxPage，不再 PageHelper.startPage")
    void resourceImplTemplate_rendersJpaPaging()
    {
        String code = render(demoTable(), "vm/java/resource-impl.java.vm");
        assertTrue(code.contains("selectDemoUserPage(demoUser, page)"));
        assertFalse(code.contains("PageUtils.startPage"));
    }

    @Test
    @DisplayName("模板清单不再包含 mapper 模板，新增 repository 模板")
    void templateList_hasNoMapper()
    {
        List<String> templates = VelocityUtils.getTemplateList(demoTable());
        assertFalse(templates.contains("vm/java/mapper.java.vm"));
        assertFalse(templates.contains("vm/xml/mapper.xml.vm"));
        assertTrue(templates.contains("vm/java/repository.java.vm"));
        assertTrue(VelocityUtils.getFileName("vm/java/repository.java.vm", demoTable())
                .endsWith("repository/DemoUserRepository.java"));
    }
}
