package com.scaffold.gen.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.constant.GenConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.text.CharsetKit;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;
import com.scaffold.gen.dao.GenCatalogDao;
import com.scaffold.gen.repository.GenTableColumnRepository;
import com.scaffold.gen.repository.GenTableRepository;
import com.scaffold.gen.util.GenUtils;
import com.scaffold.gen.util.VelocityInitializer;
import com.scaffold.gen.util.VelocityUtils;

/**
 * 业务 服务层实现
 *
 * @author ct
 */
@Service
public class GenTableServiceImpl implements IGenTableService
{
    private static final Logger log = LoggerFactory.getLogger(GenTableServiceImpl.class);

    private final GenTableRepository genTableRepository;
    private final GenTableColumnRepository genTableColumnRepository;
    private final GenCatalogDao genCatalogDao;

    public GenTableServiceImpl(GenTableRepository genTableRepository, GenTableColumnRepository genTableColumnRepository, GenCatalogDao genCatalogDao)
    {
        this.genTableRepository = genTableRepository;
        this.genTableColumnRepository = genTableColumnRepository;
        this.genCatalogDao = genCatalogDao;
    }

    /**
     * 查询业务信息
     * 
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableById(Long id)
    {
        GenTable genTable = loadByIdWithColumns(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    /**
     * 查询业务列表
     * 
     * @param genTable 业务信息
     * @return 业务集合
     */
    @Override
    public List<GenTable> selectGenTableList(GenTable genTable)
    {
        return genTableRepository.list(toSpec(genTable), Sort.unsorted());
    }

    /**
     * 分页查询业务表配置（JPA PageRequest 分页）
     */
    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectGenTablePage(
            GenTable genTable, com.scaffold.common.core.web.page.PageDomain page)
    {
        return com.scaffold.common.core.web.page.TableDataInfo.from(
                genTableRepository.page(toSpec(genTable),
                        com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /**
     * 查询据库列表（方言分支在 mapper XML 内按 _databaseId 选择，见 GenTableMapper.xml）
     *
     * @param genTable 业务信息
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableList(GenTable genTable)
    {
        return genCatalogDao.selectDbTableList(genTable);
    }

    /**
     * 查询据库列表（方言分支在 mapper XML 内按 _databaseId 选择）
     *
     * @param tableNames 表名称组
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames)
    {
        return genCatalogDao.selectDbTableListByNames(tableNames);
    }

    /**
     * 查询所有表信息
     * 
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll()
    {
        return loadAllWithColumns();
    }

    /**
     * 修改业务
     * 
     * @param genTable 业务信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGenTable(GenTable genTable)
    {
        String options = JSON.toJSONString(genTable.getParams());
        genTable.setOptions(options);
        int row = updateRow(genTable);
        if (row > 0)
        {
            for (GenTableColumn genTableColumn : genTable.getColumns())
            {
                genTableColumnRepository.save(genTableColumn);
            }
        }
    }

    /**
     * 删除业务对象
     * 
     * @param tableIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGenTableByIds(Long[] tableIds)
    {
        genTableRepository.deleteAllById(Arrays.asList(tableIds));
        for (Long tableId : tableIds)
        {
            genTableColumnRepository.deleteByTableId(tableId);
        }
    }

    /**
     * 导入表结构
     * 
     * @param tableList 导入表列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importGenTable(List<GenTable> tableList, String tplWebType)
    {
        String operName = SecurityUtils.getUsername();
        try
        {
            for (GenTable table : tableList)
            {
                String tableName = table.getTableName();
                table.setTplWebType(tplWebType);
                GenUtils.initTable(table, operName);
                int row = insertRow(table);
                if (row > 0)
                {
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = genCatalogDao.selectDbTableColumnsByName(tableName);
                    for (GenTableColumn column : genTableColumns)
                    {
                        GenUtils.initColumnField(column, table);
                        genTableColumnRepository.save(column);
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 预览代码
     * 
     * @param tableId 表编号
     * @return 预览数据列表
     */
    @Override
    public Map<String, String> previewCode(Long tableId)
    {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 查询表信息
        GenTable table = loadByIdWithColumns(tableId);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     * 
     * @param tableName 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String tableName)
    {
        return downloadCode(new String[] { tableName });
    }

    /**
     * 生成代码（自定义路径）
     * 
     * @param tableName 表名称
     */
    @Override
    public void generatorCode(String tableName)
    {
        // 查询表信息
        GenTable table = loadByNameWithColumns(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            if (!StringUtils.containsAny(template, "sql.vm", "api.ts.vm", "index.vue.vm", "index-tree.vue.vm", "view.vue.vm"))
            {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);
                try
                {
                    String path = getGenPath(table, template);
                    FileUtils.writeStringToFile(new File(path), sw.toString(), CharsetKit.UTF_8);
                }
                catch (IOException e)
                {
                    throw new ServiceException("渲染模板失败，表名：" + table.getTableName());
                }
            }
        }
    }

    /**
     * 同步数据库
     * 
     * @param tableName 表名称
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void synchDb(String tableName)
    {
        GenTable table = loadByNameWithColumns(tableName);
        List<GenTableColumn> tableColumns = table.getColumns();
        Map<String, GenTableColumn> tableColumnMap = tableColumns.stream().collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

        List<GenTableColumn> dbTableColumns = genCatalogDao.selectDbTableColumnsByName(tableName);
        if (StringUtils.isEmpty(dbTableColumns))
        {
            throw new ServiceException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = dbTableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

        dbTableColumns.forEach(column -> {
            GenUtils.initColumnField(column, table);
            if (tableColumnMap.containsKey(column.getColumnName()))
            {
                GenTableColumn prevColumn = tableColumnMap.get(column.getColumnName());
                column.setColumnId(prevColumn.getColumnId());
                if (column.isList())
                {
                    // 如果是列表，继续保留查询方式/字典类型选项
                    column.setDictType(prevColumn.getDictType());
                    column.setQueryType(prevColumn.getQueryType());
                }
                if (StringUtils.isNotEmpty(prevColumn.getIsRequired()) && !column.isPk()
                        && (column.isInsert() || column.isEdit())
                        && ((column.isUsableColumn()) || (!column.isSuperColumn())))
                {
                    // 如果是(新增/修改&非主键/非忽略及父属性)，继续保留必填/显示类型选项
                    column.setIsRequired(prevColumn.getIsRequired());
                    column.setHtmlType(prevColumn.getHtmlType());
                }
                genTableColumnRepository.save(column);
            }
            else
            {
                genTableColumnRepository.save(column);
            }
        });

        List<GenTableColumn> delColumns = tableColumns.stream().filter(column -> !dbTableColumnNames.contains(column.getColumnName())).collect(Collectors.toList());
        if (StringUtils.isNotEmpty(delColumns))
        {
            genTableColumnRepository.deleteAllById(delColumns.stream().map(GenTableColumn::getColumnId).collect(Collectors.toList()));
        }
    }

    /**
     * 批量生成代码（下载方式）
     * 
     * @param tableNames 表数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String[] tableNames)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        Map<String, StringBuffer> typeFiles = new HashMap<>();
        for (String tableName : tableNames)
        {
            generatorCode(tableName, zip, typeFiles);
        }
        for (Map.Entry<String, StringBuffer> entry : typeFiles.entrySet())
        {
            writeToZip(zip, entry.getKey(), entry.getValue().toString());
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并生成代码
     */
    private void generatorCode(String tableName, ZipOutputStream zip, Map<String, StringBuffer> typeFiles)
    {
        // 查询表信息
        GenTable table = loadByNameWithColumns(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            String fileName = VelocityUtils.getFileName(template, table);
            // index-bak.ts 模版，追加内容
            if (fileName.contains("index-bak.ts"))
            {
                if (!typeFiles.containsKey(fileName))
                {
                    typeFiles.put(fileName, new StringBuffer(sw.toString()));
                }
                else
                {
                    Arrays.stream(sw.toString().split("\n")).filter(line -> line.startsWith("export * from")).forEach(line -> typeFiles.get(fileName).append("\n").append(line));
                }
            }
            else
            {
                // 其他文件正常添加
                writeToZip(zip, fileName, sw.toString());
            }
        }
    }

    /**
     * 将字符串内容写入ZIP输出流
     * 
     * @param zip ZIP输出流
     * @param fileName ZIP条目名称（即文件名）
     * @param content 要写入的内容
     */
    private void writeToZip(ZipOutputStream zip, String fileName, String content)
    {
        try
        {
            zip.putNextEntry(new ZipEntry(fileName));
            IOUtils.write(content, zip, Constants.UTF8);
            zip.flush();
            zip.closeEntry();
        }
        catch (IOException e)
        {
            log.error("写入ZIP文件失败，文件名: " + fileName, e);
        }
    }

    /**
     * 修改保存参数校验
     * 
     * @param genTable 业务信息
     */
    @Override
    public void validateEdit(GenTable genTable)
    {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory()))
        {
            String options = JSON.toJSONString(genTable.getParams());
            JSONObject paramsObj = JSON.parseObject(options);
            if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_CODE)))
            {
                throw new ServiceException("树编码字段不能为空");
            }
            else if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_PARENT_CODE)))
            {
                throw new ServiceException("树父编码字段不能为空");
            }
            else if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_NAME)))
            {
                throw new ServiceException("树名称字段不能为空");
            }
        }
        else if (GenConstants.TPL_SUB.equals(genTable.getTplCategory()))
        {
            if (StringUtils.isEmpty(genTable.getSubTableName()))
            {
                throw new ServiceException("关联子表的表名不能为空");
            }
            else if (StringUtils.isEmpty(genTable.getSubTableFkName()))
            {
                throw new ServiceException("子表关联的外键名不能为空");
            }
        }
    }

    /**
     * 设置主键列信息
     * 
     * @param table 业务表信息
     */
    public void setPkColumn(GenTable table)
    {
        for (GenTableColumn column : table.getColumns())
        {
            if (column.isPk())
            {
                table.setPkColumn(column);
                break;
            }
        }
        if (StringUtils.isNull(table.getPkColumn()))
        {
            table.setPkColumn(table.getColumns().get(0));
        }
        if (GenConstants.TPL_SUB.equals(table.getTplCategory()))
        {
            for (GenTableColumn column : table.getSubTable().getColumns())
            {
                if (column.isPk())
                {
                    table.getSubTable().setPkColumn(column);
                    break;
                }
            }
            if (StringUtils.isNull(table.getSubTable().getPkColumn()))
            {
                table.getSubTable().setPkColumn(table.getSubTable().getColumns().get(0));
            }
        }
    }

    /**
     * 设置主子表信息
     * 
     * @param table 业务表信息
     */
    public void setSubTable(GenTable table)
    {
        String subTableName = table.getSubTableName();
        if (StringUtils.isNotEmpty(subTableName))
        {
            table.setSubTable(loadByNameWithColumns(subTableName));
        }
    }

    /**
     * 设置代码生成其他选项值
     * 
     * @param genTable 设置后的生成对象
     */
    public void setTableFromOptions(GenTable genTable)
    {
        JSONObject paramsObj = JSON.parseObject(genTable.getOptions());
        if (StringUtils.isNotNull(paramsObj))
        {
            String treeCode = paramsObj.getString(GenConstants.TREE_CODE);
            String treeParentCode = paramsObj.getString(GenConstants.TREE_PARENT_CODE);
            String treeName = paramsObj.getString(GenConstants.TREE_NAME);
            Long parentMenuId = paramsObj.getLongValue(GenConstants.PARENT_MENU_ID);
            String parentMenuName = paramsObj.getString(GenConstants.PARENT_MENU_NAME);
            boolean isView = paramsObj.getBooleanValue(GenConstants.GEN_VIEW);

            genTable.setTreeCode(treeCode);
            genTable.setTreeParentCode(treeParentCode);
            genTable.setTreeName(treeName);
            genTable.setParentMenuId(parentMenuId);
            genTable.setParentMenuName(parentMenuName);
            genTable.setView(isView);
        }
    }

    /**
     * 获取代码生成地址
     * 
     * @param table 业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    public static String getGenPath(GenTable table, String template)
    {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/"))
        {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + VelocityUtils.getFileName(template, table);
        }
        return genPath + File.separator + VelocityUtils.getFileName(template, table);
    }

    /**
     * 加载业务表并回填字段列表（对齐原 selectGenTableById 的 LEFT JOIN gen_table_column）。
     */
    private GenTable loadByIdWithColumns(Long tableId)
    {
        GenTable table = genTableRepository.findById(tableId).orElse(null);
        if (table != null)
        {
            table.setColumns(genTableColumnRepository.findByTableIdOrderBySortAsc(tableId));
        }
        return table;
    }

    /**
     * 按表名加载业务表并回填字段列表（对齐原 selectGenTableByName）。
     */
    private GenTable loadByNameWithColumns(String tableName)
    {
        GenTable table = genTableRepository.findByTableName(tableName);
        if (table != null)
        {
            table.setColumns(genTableColumnRepository.findByTableIdOrderBySortAsc(table.getTableId()));
        }
        return table;
    }

    /**
     * 全量业务表并回填字段列表（对齐原 selectGenTableAll）。
     */
    private List<GenTable> loadAllWithColumns()
    {
        List<GenTable> tables = genTableRepository.findAll();
        for (GenTable table : tables)
        {
            table.setColumns(genTableColumnRepository.findByTableIdOrderBySortAsc(table.getTableId()));
        }
        return tables;
    }

    /**
     * 动态条件（对齐原 selectGenTableList 的 if 分支）。
     */
    private Specification<Object> toSpec(GenTable genTable)
    {
        if (genTable == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("tableName", genTable.getTableName())
                .and(JpaSpecs.likeIf("tableComment", genTable.getTableComment()))
                .and(JpaSpecs.dateRangeIf("createTime", genTable.getParams()));
    }

    /** 新增（IDENTITY 回填主键，恒 1 行） */
    private int insertRow(GenTable table)
    {
        genTableRepository.save(table);
        return 1;
    }

    /** 更新（原 updateGenTable 动态列对齐：全字段由前端编辑表单提交） */
    private int updateRow(GenTable genTable)
    {
        genTableRepository.save(genTable);
        return 1;
    }
}
