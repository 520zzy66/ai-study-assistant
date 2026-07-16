/**
 * 文件夹树 + 资料列表 → 级联选择器数据构建工具
 *
 * 将文件夹树和资料列表合并为 el-cascader 所需的 options 格式
 * 支持多层嵌套，资料和文件夹混排（文件夹在前，资料在后）
 */

/**
 * 构建级联选择器选项
 * @param {Array} folderTree 文件夹树（后端返回的嵌套结构）
 * @param {Array} materialList 资料列表（扁平结构，含 folderId）
 * @returns {Array} el-cascader options
 */
export function buildCascaderOptions(folderTree, materialList) {
  const options = []

  // 1. 先收集未分类资料（folderId 为 null 或 undefined）
  const uncategorizedMaterials = materialList.filter(m => !m.folderId)

  // 2. 构建文件夹节点（递归）
  function buildFolderNode(folder) {
    // 找出该文件夹下的直属资料
    const folderMaterials = materialList.filter(m => m.folderId === folder.id)

    const node = {
      value: `folder_${folder.id}`,
      label: folder.name,
      isLeaf: false,
      isFolder: true,
      folderId: folder.id,
      materialCount: folder.materialCount || 0,
      children: []
    }

    // 递归处理子文件夹
    if (folder.children && folder.children.length > 0) {
      for (const child of folder.children) {
        node.children.push(buildFolderNode(child))
      }
    }

    // 添加该文件夹下的直属资料
    for (const mat of folderMaterials) {
      node.children.push({
        value: `material_${mat.id}`,
        label: mat.originalName,
        isLeaf: true,
        isFolder: false,
        materialId: mat.id,
        fileType: mat.fileType,
        fileSize: mat.fileSize,
        status: mat.status
      })
    }

    // 如果没有子项，标记为叶子节点（但仍可选择文件夹本身）
    if (node.children.length === 0) {
      node.isLeaf = true
    }

    return node
  }

  // 3. 构建根级别的文件夹
  for (const folder of folderTree) {
    options.push(buildFolderNode(folder))
  }

  // 4. 添加未分类资料到根级别
  for (const mat of uncategorizedMaterials) {
    options.push({
      value: `material_${mat.id}`,
      label: mat.originalName,
      isLeaf: true,
      isFolder: false,
      materialId: mat.id,
      fileType: mat.fileType,
      fileSize: mat.fileSize,
      status: mat.status
    })
  }

  return options
}

/**
 * 从级联选择器的值路径中解析选中项信息
 * @param {Array} valuePath cascader 的值路径，如 ['folder_1', 'folder_2', 'material_3']
 * @param {Array} options cascader 的 options
 * @returns {Object} { type: 'folder'|'material', id: number, path: string[] }
 */
export function parseCascaderValue(valuePath, options) {
  if (!valuePath || valuePath.length === 0) return null

  const lastValue = valuePath[valuePath.length - 1]
  const isFolder = lastValue.startsWith('folder_')
  const id = Number(lastValue.replace(/^(folder|material)_/, ''))

  // 构建显示路径
  const pathLabels = []
  let currentOptions = options

  for (const val of valuePath) {
    const item = currentOptions.find(opt => opt.value === val)
    if (item) {
      pathLabels.push(item.label)
      currentOptions = item.children || []
    }
  }

  return {
    type: isFolder ? 'folder' : 'material',
    id,
    path: pathLabels
  }
}

/**
 * 获取级联选择器的显示标签
 * @param {Array} valuePath cascader 的值路径
 * @param {Array} options cascader 的 options
 * @returns {string} 显示路径，如 "考公 / 行测 / 资料A.pdf"
 */
export function getCascaderLabel(valuePath, options) {
  const parsed = parseCascaderValue(valuePath, options)
  if (!parsed) return ''
  return parsed.path.join(' / ')
}
