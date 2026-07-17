<template>
  <div class="material-page">
    <BasePageHeader
      title="学习资料"
      description="管理你的学习资料，支持文件夹分类、AI 总结、问答和出题"
    >
      <template #actions>
        <el-button type="primary" @click="showUpload = true">
          <el-icon><Upload /></el-icon>
          上传资料
        </el-button>
      </template>
    </BasePageHeader>

    <!-- 主体布局：左侧文件夹树 + 右侧内容区 -->
    <div class="material-layout">
      <!-- ========== 左侧文件夹树 ========== -->
      <aside class="folder-sidebar" aria-label="资料文件夹">
        <div class="folder-header">
          <span class="folder-header-title">文件夹</span>
          <el-button type="primary" text size="small" aria-label="新建文件夹" @click="showCreateFolderDialog()">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>

        <!-- 根节点（全部资料） -->
        <div
          class="folder-item folder-root"
          :class="{ active: !currentFolderId }"
            role="button"
            tabindex="0"
            @click="selectFolder(null)"
            @keydown.enter="selectFolder(null)"
        >
          <el-icon><HomeFilled /></el-icon>
          <span class="folder-name">全部资料</span>
          <span class="folder-count">{{ totalMaterialCount }}</span>
        </div>

        <!-- 文件夹树 -->
        <el-tree
          ref="folderTreeRef"
          :data="folderTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          class="folder-tree"
          @node-click="handleFolderClick"
        >
          <template #default="{ node, data }">
            <div class="folder-node">
              <el-icon class="folder-node-icon"><Folder /></el-icon>
              <span class="folder-node-name">{{ node.label }}</span>
              <span class="folder-node-count">{{ data.materialCount || 0 }}</span>
              <div class="folder-node-actions" @click.stop>
                <el-button size="small" text type="primary" @click.stop="handleFolderSummary(data)" style="padding: 0 4px; margin-right: 4px;">
                  {{ data.summary ? '查看总结' : '总结' }}
                </el-button>
                <el-dropdown trigger="click" @command="(cmd) => handleFolderCommand(cmd, data)">
                  <el-icon class="more-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">
                        <el-icon><Edit /></el-icon> 重命名
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <span style="color: var(--el-color-danger)">
                          <el-icon><Delete /></el-icon> 删除
                        </span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </template>
        </el-tree>
      </aside>

      <!-- ========== 右侧内容区 ========== -->
      <div class="content-main">
        <!-- Tabs -->
        <div class="material-tabs">
          <button
            class="tab-btn"
            type="button"
            :class="{ active: activeTab === 'mine' }"
            @click="activeTab = 'mine'"
          >
            <el-icon><Document /></el-icon>
            我的资料
          </button>
          <button
            class="tab-btn"
            type="button"
            :class="{ active: activeTab === 'library' }"
            @click="activeTab = 'library'; loadLibrary()"
          >
            <el-icon><Collection /></el-icon>
            资料库
          </button>
          <button
            class="tab-btn"
            type="button"
            :class="{ active: activeTab === 'temporary' }"
            @click="activeTab = 'temporary'; loadTemporaryMaterials()"
          >
            <el-icon><Clock /></el-icon>
            临时资料
          </button>
        </div>

        <!-- ========== 我的资料 Tab ========== -->
        <template v-if="activeTab === 'mine'">
          <!-- Toolbar -->
          <div class="material-toolbar">
            <div class="toolbar-left">
              <!-- 面包屑导航 -->
              <div class="breadcrumb-nav">
                <span class="breadcrumb-item" @click="selectFolder(null)">全部资料</span>
                <template v-for="item in breadcrumbPath" :key="item.id">
                  <el-icon class="breadcrumb-sep"><ArrowRight /></el-icon>
                  <span class="breadcrumb-item" @click="selectFolder(item.id)">{{ item.name }}</span>
                </template>
              </div>
            </div>
            <div class="toolbar-right">
              <el-input
                v-model="queryParams.keyword"
                placeholder="搜索资料名称..."
                clearable
                style="width:200px;"
                @keyup.enter="handleSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width:120px;" @change="handleSearch">
                <el-option label="处理中" value="processing" />
                <el-option label="可用" value="ready" />
                <el-option label="失败" value="failed" />
              </el-select>
            </div>
          </div>

          <!-- Content Card -->
          <BaseCard class="material-list-panel" :padding="'none'">
            <!-- Skeleton Loading -->
            <div v-if="loading" class="table-loading">
              <el-skeleton :rows="8" animated />
            </div>

            <!-- Empty -->
            <div v-else-if="materialList.length === 0" style="padding:40px 0;">
              <AppEmpty icon="Document" title="暂无学习资料" description="上传你的第一份资料，开始学习之旅">
                <template #action>
                  <el-button type="primary" @click="showUpload = true">上传资料</el-button>
                </template>
              </AppEmpty>
            </div>

            <!-- Table -->
            <template v-else>
              <el-table
                ref="materialTableRef"
                :data="materialList"
                v-loading="loading"
                style="width: 100%"
                row-key="id"
                class="material-table"
                @row-dblclick="handleRowDblClick"
                @selection-change="handleSelectionChange"
              >
                <el-table-column type="selection" width="55" align="center" />
                <el-table-column label="文件名" min-width="240">
                  <template #default="{ row }">
                    <div class="file-cell">
                      <div class="file-icon" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                        <el-icon :size="18"><Document /></el-icon>
                      </div>
                      <div class="file-info">
                        <div class="file-name truncate">{{ row.originalName }}</div>
                        <div class="file-size">{{ formatFileSize(row.fileSize) }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="100">
                  <template #default="{ row }">
                    <span class="file-type-badge" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                      {{ row.fileType?.toUpperCase() || '-' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="所在文件夹" width="120">
                  <template #default="{ row }">
                    <span class="folder-badge" v-if="row.folderName">
                      <el-icon><Folder /></el-icon> {{ row.folderName }}
                    </span>
                    <span v-else class="text-muted">未分类</span>
                  </template>
                </el-table-column>
                <el-table-column label="上传时间" width="170">
                  <template #default="{ row }">
                    <span class="time-text">{{ row.createTime }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                      {{ getStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right" align="right">
                  <template #default="{ row }">
                    <div class="action-btns">
                      <el-button v-if="row.status === 'ready'" size="small" type="primary" plain @click.stop="goToChat(row)">AI 问答</el-button>
                      <el-dropdown trigger="click" @command="(command) => handleMaterialCommand(command, row)" @click.stop>
                        <button class="row-more-button" type="button" :aria-label="`${row.originalName}的更多操作`">
                          <el-icon><MoreFilled /></el-icon>
                        </button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="detail">
                              <el-icon><Document /></el-icon> 查看详情
                            </el-dropdown-item>
                            <el-dropdown-item v-if="row.status === 'ready'" command="summary">
                              <el-icon><Collection /></el-icon> {{ row.summary ? '查看总结' : '生成总结' }}
                            </el-dropdown-item>
                            <el-dropdown-item v-if="row.status === 'ready'" command="mindmap">
                              <el-icon><Connection /></el-icon> 思维导图
                            </el-dropdown-item>
                            <el-dropdown-item command="delete" divided>
                              <el-icon><Delete /></el-icon> 删除资料
                            </el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 批量操作栏 -->
              <div v-if="selectedMaterials.length > 0" class="batch-bar">
                <span class="batch-info">已选择 {{ selectedMaterials.length }} 项</span>
                <div class="batch-actions">
                  <el-button size="small" @click="showMoveDialog">
                    <el-icon><Rank /></el-icon> 批量移动
                  </el-button>
                  <el-button size="small" type="danger" @click="handleBatchDelete">
                    <el-icon><Delete /></el-icon> 批量删除
                  </el-button>
                </div>
              </div>

              <!-- Pagination -->
              <div v-if="total > 0" class="table-footer">
                <span class="total-text">共 {{ total }} 份资料</span>
                <el-pagination
                  v-model:current-page="queryParams.page"
                  v-model:page-size="queryParams.size"
                  :page-sizes="[10, 20, 50]"
                  :total="total"
                  layout="sizes, prev, pager, next"
                  @size-change="handleSearch"
                  @current-change="handleSearch"
                />
              </div>
            </template>
          </BaseCard>
        </template>

        <!-- ========== 资料库 Tab ========== -->
        <template v-if="activeTab === 'library'">
          <!-- Toolbar -->
          <div class="material-toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="libraryQuery.keyword"
                placeholder="搜索资料名称..."
                clearable
                style="width:240px;"
                @keyup.enter="loadLibrary"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-select v-model="libraryQuery.category" placeholder="全部分类" clearable style="width:140px;" @change="loadLibrary">
                <el-option label="学习方法" value="学习方法" />
                <el-option label="计算机基础" value="计算机基础" />
                <el-option label="英语学习" value="英语学习" />
              </el-select>
            </div>
          </div>

          <!-- Content Card -->
          <BaseCard class="material-list-panel" :padding="'none'">
            <div v-if="libraryLoading" class="table-loading">
              <el-skeleton :rows="8" animated />
            </div>

            <div v-else-if="libraryList.length === 0" style="padding:40px 0;">
              <AppEmpty icon="Collection" title="暂无资料" description="资料库正在建设中，敬请期待" />
            </div>

            <template v-else>
              <el-table :data="libraryList" class="material-table" @row-click="handleLibraryView">
                <el-table-column label="资料名称" min-width="260">
                  <template #default="{ row }">
                    <div class="file-cell">
                      <div class="file-icon" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                        <el-icon :size="18"><Document /></el-icon>
                      </div>
                      <div class="file-info">
                        <div class="file-name truncate">{{ row.originalName }}</div>
                        <div class="file-desc">{{ row.summary ? row.summary.substring(0, 60) + '...' : row.category }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="分类" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain">{{ row.category || '-' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="切片数" width="80" align="center">
                  <template #default="{ row }">
                    <span class="time-text">{{ row.chunkCount || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="300" fixed="right">
                  <template #default="{ row }">
                    <div class="action-btns">
                      <el-button size="small" text type="success" @click.stop="handleAddToMine(row)">
                        <el-icon><Plus /></el-icon> 添加到我的资料
                      </el-button>
                      <el-button size="small" text type="primary" @click.stop="goToSummary(row)">总结</el-button>
                      <el-button size="small" text type="primary" @click.stop="goToChat(row)">问答</el-button>
                      <el-button size="small" text type="warning" @click.stop="goToMindmap(row)">
                        <el-icon><Connection /></el-icon> 导图
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="libraryTotal > 0" class="table-footer">
                <span class="total-text">共 {{ libraryTotal }} 份资料</span>
                <el-pagination
                  v-model:current-page="libraryQuery.page"
                  v-model:page-size="libraryQuery.size"
                  :page-sizes="[10, 20, 50]"
                  :total="libraryTotal"
                  layout="sizes, prev, pager, next"
                  @size-change="loadLibrary"
                  @current-change="loadLibrary"
                />
              </div>
            </template>
          </BaseCard>
        </template>

        <!-- ========== 临时资料 Tab ========== -->
        <template v-if="activeTab === 'temporary'">
          <div class="material-toolbar temporary-toolbar">
            <div class="toolbar-left">
              <span class="temporary-hint">问答附件默认保留 7 天，添加到我的资料后转为长期保存。</span>
            </div>
            <div class="toolbar-right">
              <el-button :loading="temporaryLoading" @click="loadTemporaryMaterials">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </div>

          <BaseCard class="material-list-panel" :padding="'none'">
            <div v-if="temporaryLoading" class="table-loading">
              <el-skeleton :rows="6" animated />
            </div>
            <div v-else-if="temporaryList.length === 0" class="temporary-empty">
              <AppEmpty icon="Clock" title="暂无临时资料" description="在 AI 问答中上传附件后，会在这里保留 7 天" />
            </div>
            <el-table v-else :data="temporaryList" class="material-table">
              <el-table-column label="资料名称" min-width="260">
                <template #default="{ row }">
                  <div class="file-cell">
                    <div class="file-icon" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                      <el-icon :size="18"><Document /></el-icon>
                    </div>
                    <div class="file-info">
                      <div class="file-name truncate">{{ row.originalName }}</div>
                      <div class="file-desc">来自会话 · {{ row.conversationId?.slice(0, 8) }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                    {{ getStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="到期时间" width="180">
                <template #default="{ row }"><span class="time-text">{{ formatExpiry(row.expiresAt) }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="250" fixed="right" align="right">
                <template #default="{ row }">
                  <div class="action-btns">
                    <el-button v-if="row.status === 'ready'" size="small" type="primary" plain @click="goToTemporaryChat(row)">AI 问答</el-button>
                    <el-button v-if="row.status === 'ready'" size="small" text @click="openPromoteDialog(row)">添加至我的资料</el-button>
                    <button class="row-more-button" type="button" :aria-label="`删除临时资料${row.originalName}`" @click="handleTemporaryDelete(row)">
                      <el-icon><Delete /></el-icon>
                    </button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </BaseCard>
        </template>
      </div>
    </div>

    <!-- ========== 弹窗 ========== -->

    <!-- Upload Dialog -->
    <el-dialog v-model="showUpload" title="上传资料" width="520px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-position="top">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx,.txt,.md"
            :on-change="handleFileChange"
            drag
          >
            <el-icon class="upload-icon" :size="36"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、TXT、Markdown 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="选择文件夹">
          <el-tree-select
            v-model="uploadForm.folderId"
            :data="folderTreeForSelect"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择存放位置（可选）"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类（可选）">
          <el-input v-model="uploadForm.category" placeholder="输入分类便于管理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="promoteDialogVisible" title="添加至我的资料" width="440px" :close-on-click-modal="false">
      <el-form :model="promoteForm" label-position="top">
        <el-form-item label="临时资料">
          <el-input :model-value="promotingMaterial?.originalName" disabled />
        </el-form-item>
        <el-form-item label="存放文件夹">
          <el-tree-select
            v-model="promoteForm.folderId"
            :data="folderTreeForSelect"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择存放位置（可选）"
            clearable check-strictly style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类（可选）">
          <el-input v-model="promoteForm.category" maxlength="50" placeholder="输入分类便于管理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="promoteSubmitting" @click="handlePromoteTemporary">添加</el-button>
      </template>
    </el-dialog>

    <!-- 创建/重命名文件夹 Dialog -->
    <el-dialog
      v-model="folderDialogVisible"
      :title="folderDialogTitle"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="folderForm" label-position="top">
        <el-form-item label="文件夹名称" required>
          <el-input
            v-model="folderForm.name"
            placeholder="请输入文件夹名称"
            maxlength="50"
            show-word-limit
            @keyup.enter="handleFolderSubmit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="folderSubmitting" @click="handleFolderSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动资料到文件夹 Dialog -->
    <el-dialog v-model="moveDialogVisible" title="移动到文件夹" width="400px">
      <el-tree-select
        v-model="moveTargetFolderId"
        :data="folderTreeForSelect"
        :props="{ label: 'name', value: 'id', children: 'children' }"
        placeholder="选择目标文件夹"
        clearable
        check-strictly
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="moveSubmitting" @click="handleMoveSubmit">移动</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailVisible" size="420px">
      <template #header>
        <span class="drawer-title">资料详情</span>
      </template>
      <div v-if="selectedMaterial" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">文件名</span>
          <span class="detail-value">{{ selectedMaterial.originalName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span class="detail-value">{{ selectedMaterial.fileType?.toUpperCase() || '-' }}</span>
        </div>
        <div v-if="selectedMaterial.fileSize" class="detail-row">
          <span class="detail-label">大小</span>
          <span class="detail-value">{{ formatFileSize(selectedMaterial.fileSize) }}</span>
        </div>
        <div v-if="selectedMaterial.category" class="detail-row">
          <span class="detail-label">分类</span>
          <span class="detail-value">{{ selectedMaterial.category }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">所在文件夹</span>
          <span class="detail-value">{{ selectedMaterial.folderName || '未分类' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="getStatusType(selectedMaterial.status)" size="small" effect="light">
            {{ getStatusLabel(selectedMaterial.status) }}
          </el-tag>
        </div>
        <div v-if="selectedMaterial.createTime" class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ selectedMaterial.createTime }}</span>
        </div>
        <div v-if="selectedMaterial.chunkCount" class="detail-row">
          <span class="detail-label">切片数</span>
          <span class="detail-value">{{ selectedMaterial.chunkCount }}</span>
        </div>
        <div v-if="selectedMaterial.summary" class="detail-row">
          <span class="detail-label">摘要</span>
          <span class="detail-value detail-summary">{{ selectedMaterial.summary }}</span>
        </div>
        <div class="detail-actions">
          <el-button v-if="selectedMaterial.status === 'ready'" type="primary" @click="handleMaterialSummary(selectedMaterial)">
            {{ selectedMaterial.summary ? '查看 AI 总结' : '生成 AI 总结' }}
          </el-button>
          <el-button v-if="selectedMaterial.status === 'ready'" @click="goToChat(selectedMaterial)">AI 问答</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 查看总结弹窗 -->
    <el-dialog v-model="summaryDialogVisible" :title="summaryDialogTitle" width="800px" center>
      <div class="summary-dialog-content">
        <div class="summary-content markdown-body" v-html="renderMarkdown(summaryContent)" v-if="summaryContent"></div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="downloadSummaryTxt" :disabled="!summaryContent">
            <el-icon><Download /></el-icon> 下载为 TXT
          </el-button>
          <el-button type="primary" @click="summaryDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 资料预览弹窗 -->
    <FilePreview
      v-model="filePreviewVisible"
      :file-id="filePreviewId"
      :file-name="filePreviewName"
      :file-type="filePreviewType"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMaterialList, uploadMaterial, deleteMaterial, getMaterialDetail,
  getMaterialLibrary, copyToMyLibrary, getTemporaryMaterials,
  deleteTemporaryMaterial, promoteTemporaryMaterial
} from '@/api/material'
import { getFolderTree, createFolder, updateFolder, deleteFolder, moveMaterialsToFolder } from '@/api/materialFolder'
import {
  Upload, UploadFilled, Search, Document, Collection, Plus,
  Folder, FolderAdd, HomeFilled, MoreFilled, Edit, Delete,
  ArrowRight, Rank, Connection, Download, Clock, Refresh
} from '@element-plus/icons-vue'
import { formatFileSize, getStatusLabel, getStatusType } from '@/utils/format'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import FilePreview from '@/components/common/FilePreview.vue'
import { useMarkdown } from '@/composables/useMarkdown'

const router = useRouter()
const activeTab = ref('mine')
const { renderMarkdown } = useMarkdown()

// ========== 文件预览相关 ==========
const filePreviewVisible = ref(false)
const filePreviewId = ref(null)
const filePreviewName = ref('')
const filePreviewType = ref('')

// ========== 总结弹窗相关 ==========
const summaryDialogVisible = ref(false)
const summaryDialogTitle = ref('')
const summaryContent = ref('')

// ========== 文件夹相关 ==========
const folderTree = ref([])
const currentFolderId = ref(null)
const folderTreeRef = ref(null)
const totalMaterialCount = ref(0)

// 文件夹弹窗
const folderDialogVisible = ref(false)
const folderDialogTitle = ref('新建文件夹')
const folderSubmitting = ref(false)
const folderForm = reactive({
  name: '',
  parentId: null,
  editId: null // 编辑模式时的文件夹ID
})

// 移动弹窗
const moveDialogVisible = ref(false)
const moveTargetFolderId = ref(null)
const moveSubmitting = ref(false)
const materialsToMove = ref([])

// 面包屑路径
const breadcrumbPath = ref([])

// ========== 资料相关 ==========
const loading = ref(false)
const uploading = ref(false)
const showUpload = ref(false)
const detailVisible = ref(false)
const materialList = ref([])
const total = ref(0)
const uploadRef = ref(null)
const materialTableRef = ref(null)
const selectedMaterial = ref(null)
const selectedMaterials = ref([])

const queryParams = reactive({ keyword: '', status: '', folderId: null, page: 1, size: 10 })
const uploadForm = reactive({ file: null, category: '', folderId: null })

// ========== 资料库 ==========
const libraryLoading = ref(false)
const libraryList = ref([])
const libraryTotal = ref(0)
const libraryQuery = reactive({ keyword: '', category: '', page: 1, size: 10 })

// ========== 临时资料 ==========
const temporaryLoading = ref(false)
const temporaryList = ref([])
const promoteDialogVisible = ref(false)
const promoteSubmitting = ref(false)
const promotingMaterial = ref(null)
const promoteForm = reactive({ folderId: null, category: '' })

// ========== 计算属性 ==========

// 文件夹树（用于下拉选择，添加"根目录"选项）
const folderTreeForSelect = computed(() => {
  return [
    { id: null, name: '根目录（未分类）', children: [] },
    ...folderTree.value
  ]
})

// ========== 文件夹操作 ==========

/** 加载文件夹树 */
async function loadFolderTree() {
  try {
    const data = await getFolderTree()
    folderTree.value = data || []
    // 计算总数
    totalMaterialCount.value = folderTree.value.reduce((sum, f) => sum + (f.materialCount || 0), 0)
  } catch (error) {
    console.error('加载文件夹树失败:', error)
  }
}

/** 选择文件夹 */
function selectFolder(folderId) {
  currentFolderId.value = folderId
  queryParams.folderId = folderId
  queryParams.page = 1

  // 更新面包屑
  if (folderId) {
    breadcrumbPath.value = findFolderPath(folderTree.value, folderId)
  } else {
    breadcrumbPath.value = []
  }

  handleSearch()
}

/** 查找文件夹路径（用于面包屑） */
function findFolderPath(tree, targetId, path = []) {
  for (const node of tree) {
    if (node.id === targetId) {
      return [...path, { id: node.id, name: node.name }]
    }
    if (node.children?.length) {
      const found = findFolderPath(node.children, targetId, [...path, { id: node.id, name: node.name }])
      if (found) return found
    }
  }
  return null
}

/** 点击文件夹树节点 */
function handleFolderClick(data) {
  selectFolder(data.id)
}

/** 显示创建文件夹对话框 */
function showCreateFolderDialog(parentId = null) {
  folderDialogTitle.value = parentId ? '新建子文件夹' : '新建文件夹'
  folderForm.name = ''
  folderForm.parentId = parentId
  folderForm.editId = null
  folderDialogVisible.value = true
}

/** 显示重命名对话框 */
function showRenameFolderDialog(data) {
  folderDialogTitle.value = '重命名文件夹'
  folderForm.name = data.name
  folderForm.parentId = data.parentId
  folderForm.editId = data.id
  folderDialogVisible.value = true
}

/** 提交文件夹表单（创建/重命名） */
async function handleFolderSubmit() {
  if (!folderForm.name?.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  folderSubmitting.value = true
  try {
    if (folderForm.editId) {
      // 重命名
      await updateFolder(folderForm.editId, { name: folderForm.name.trim() })
      ElMessage.success('重命名成功')
    } else {
      // 创建
      await createFolder({
        name: folderForm.name.trim(),
        parentId: folderForm.parentId
      })
      ElMessage.success('创建成功')
    }
    folderDialogVisible.value = false
    await loadFolderTree()
  } catch (error) {
    ElMessage.error(folderForm.editId ? '重命名失败' : '创建失败')
  } finally {
    folderSubmitting.value = false
  }
}

/** 处理文件夹下拉命令 */
function handleFolderCommand(cmd, data) {
  switch (cmd) {
    case 'rename':
      showRenameFolderDialog(data)
      break
    case 'createSub':
      showCreateFolderDialog(data.id)
      break
    case 'delete':
      handleDeleteFolder(data)
      break
  }
}

/** 删除文件夹 */
async function handleDeleteFolder(data) {
  await ElMessageBox.confirm(
    `确定删除文件夹"${data.name}"吗？文件夹内的资料将变为未分类状态。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
  )

  try {
    await deleteFolder(data.id)
    ElMessage.success('删除成功')
    // 如果删除的是当前选中的文件夹，回到全部资料
    if (currentFolderId.value === data.id) {
      selectFolder(null)
    }
    await loadFolderTree()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// ========== 资料操作 ==========

async function handleSearch() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.keyword) { params.fileName = params.keyword; delete params.keyword }
    const data = await getMaterialList(params)
    materialList.value = data.records || []
    total.value = data.total || 0
  } catch { materialList.value = [] }
  finally { loading.value = false }
}

function handleFileChange(file) { uploadForm.file = file.raw }

async function handleUpload() {
  if (!uploadForm.file) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    if (uploadForm.category) formData.append('category', uploadForm.category)
    if (uploadForm.folderId) formData.append('folderId', uploadForm.folderId)
    await uploadMaterial(formData)
    ElMessage.success('上传成功')
    showUpload.value = false
    uploadForm.file = null; uploadForm.category = ''; uploadForm.folderId = null
    uploadRef.value?.clearFiles()
    handleSearch()
    loadFolderTree() // 刷新文件夹计数
  } finally { uploading.value = false }
}

function handleDetail(row) { selectedMaterial.value = row; detailVisible.value = true }

/** 资料行更多操作，保持原有业务行为不变。 */
function handleMaterialCommand(command, row) {
  const actions = {
    detail: () => handleDetail(row),
    summary: () => handleMaterialSummary(row),
    mindmap: () => goToMindmap(row),
    delete: () => handleDelete(row)
  }
  actions[command]?.()
}

function handleRowDblClick(row) {
  filePreviewId.value = row.id
  filePreviewName.value = row.originalName
  filePreviewType.value = row.fileType || ''
  filePreviewVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该资料？删除后不可恢复。', '确认删除', { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' })
  try {
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    handleSearch()
    loadFolderTree()
  } catch { /* handled */ }
}
/** 显示批量移动对话框 */
function showMoveDialog() {
  materialsToMove.value = selectedMaterials.value.map(r => r.id)
  moveTargetFolderId.value = null
  moveDialogVisible.value = true
}

/** 提交移动 */
async function handleMoveSubmit() {
  moveSubmitting.value = true
  try {
    await moveMaterialsToFolder(materialsToMove.value, moveTargetFolderId.value)
    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    selectedMaterials.value = []
    materialTableRef.value?.clearSelection()
    handleSearch()
    loadFolderTree()
  } catch (error) {
    ElMessage.error('移动失败')
  } finally {
    moveSubmitting.value = false
  }
}

/** 批量删除 */
async function handleBatchDelete() {
  await ElMessageBox.confirm(
    `确定删除选中的 ${selectedMaterials.value.length} 份资料？删除后不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
  )

  try {
    for (const item of selectedMaterials.value) {
      await deleteMaterial(item.id)
    }
    ElMessage.success('批量删除成功')
    selectedMaterials.value = []
    materialTableRef.value?.clearSelection()
    handleSearch()
    loadFolderTree()
  } catch (error) {
    ElMessage.error('部分资料删除失败')
  }
}

/** 表格选择变化 */
function handleSelectionChange(selection) {
  selectedMaterials.value = selection
}

async function handleMaterialSummary(row) {
  if (row.summary) {
    // 查看总结
    summaryDialogTitle.value = `总结 - ${row.originalName}`
    summaryContent.value = row.summary
    summaryDialogVisible.value = true
  } else {
    // 生成总结，跳转到 AI 总结页面
    router.push({
      path: '/ai/summary',
      query: { materialId: row.id }
    })
  }
}

async function handleFolderSummary(folder) {
  if (folder.summary) {
    // 查看总结
    summaryDialogTitle.value = `总结 - ${folder.name}`
    summaryContent.value = folder.summary
    summaryDialogVisible.value = true
  } else {
    // 生成总结
    router.push({
      path: '/ai/summary',
      query: { folderId: folder.id }
    })
  }
}

function goToChat(row) { router.push(`/ai/chat?materialId=${row.id}`) }
/** 前往思维导图工作台 */
function goToMindmap(row) {
  if (!row.summary && !row.mindMap) {
    ElMessage.warning('该资料还未进行 AI 总结，请先生成总结！')
    return
  }
  router.push(`/ai/mindmap?materialId=${row.id}`)
}

/** 跳转到总结 (针对资料库的复用) */
function goToSummary(row) {
  handleMaterialSummary(row)
}

/** 将总结文本下载为 TXT */
function downloadSummaryTxt() {
  if (!summaryContent.value) return
  // 去除原始文件后缀，防止出现 "费曼.txt.md" 这类情况
  const rawTitle = summaryDialogTitle.value.replace(/^总结 - /, '') || 'AI总结'
  const cleanTitle = rawTitle.replace(/\.[^.]+$/, '') // 去掉最后一个扩展名
  const blob = new Blob([summaryContent.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${cleanTitle}_AI总结.txt`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function handleAddToMine(row) {
  try {
    await copyToMyLibrary(row.id)
    ElMessage.success('已添加到我的资料库')
    handleSearch()
  } catch { /* handled by interceptor */ }
}

// ========== 资料库 ==========

async function loadLibrary() {
  libraryLoading.value = true
  try {
    const data = await getMaterialLibrary(libraryQuery)
    libraryList.value = data.records || []
    libraryTotal.value = data.total || 0
  } catch { libraryList.value = [] }
  finally { libraryLoading.value = false }
}

async function handleLibraryView(row) {
  try {
    const data = await getMaterialDetail(row.id)
    selectedMaterial.value = data
    detailVisible.value = true
  } catch { /* handled */ }
}

async function loadTemporaryMaterials() {
  temporaryLoading.value = true
  try {
    temporaryList.value = await getTemporaryMaterials() || []
  } catch {
    temporaryList.value = []
  } finally {
    temporaryLoading.value = false
  }
}

function goToTemporaryChat(row) {
  router.push({
    path: '/ai/chat',
    query: { temporaryMaterialToken: row.uploadToken, conversationId: row.conversationId }
  })
}

function openPromoteDialog(row) {
  promotingMaterial.value = row
  promoteForm.folderId = null
  promoteForm.category = ''
  promoteDialogVisible.value = true
}

async function handlePromoteTemporary() {
  if (!promotingMaterial.value) return
  promoteSubmitting.value = true
  try {
    await promoteTemporaryMaterial(promotingMaterial.value.uploadToken, {
      folderId: promoteForm.folderId,
      category: promoteForm.category?.trim() || null
    })
    ElMessage.success('已添加到我的资料')
    promoteDialogVisible.value = false
    await Promise.all([loadTemporaryMaterials(), handleSearch(), loadFolderTree()])
  } finally {
    promoteSubmitting.value = false
  }
}

async function handleTemporaryDelete(row) {
  await ElMessageBox.confirm(`确定删除临时资料“${row.originalName}”吗？`, '删除临时资料', {
    type: 'warning', confirmButtonText: '删除'
  })
  await deleteTemporaryMaterial(row.uploadToken)
  ElMessage.success('临时资料已删除')
  loadTemporaryMaterials()
}

function formatExpiry(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

// ========== 工具函数 ==========

function getFileIconBg(type) {
  const map = { pdf: '#fef2f2', doc: '#eff6ff', docx: '#eff6ff', epub: '#fffbeb', md: '#f3f4f6', txt: '#f3f4f6' }
  return map[(type || '').toLowerCase()] || '#f3f4f6'
}

function getFileIconColor(type) {
  const map = { pdf: '#dc2626', doc: '#2563eb', docx: '#2563eb', epub: '#d97706', md: '#6b7280', txt: '#6b7280' }
  return map[(type || '').toLowerCase()] || '#6b7280'
}

// ========== 初始化 ==========

onMounted(async () => {
  await Promise.all([
    loadFolderTree(),
    handleSearch()
  ])
})
</script>

<style scoped>
.material-page { width: 100%; }

/* ========== 主体布局 ========== */
.material-layout {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 200px);
}

/* ========== 左侧文件夹树 ========== */
.folder-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--outline-variant);
  overflow: hidden;
}

.folder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.folder-header-title {
  font-size: var(--text-ui);
  font-weight: 600;
  color: var(--color-text-primary);
}

.folder-root {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all var(--duration-fast);
  border-bottom: 1px solid var(--outline-variant);
}

.folder-root:hover {
  background: var(--surface-container);
}

.folder-root.active {
  background: var(--color-primary-light-9);
  color: var(--color-primary);
}

.folder-root .el-icon {
  font-size: 18px;
}

.folder-name {
  flex: 1;
  font-size: var(--text-ui);
  font-weight: 500;
}

.folder-count {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  background: var(--surface-container);
  padding: 2px 8px;
  border-radius: 10px;
}

.folder-tree {
  padding: 8px;
}

.folder-tree :deep(.el-tree-node__content) {
  height: 40px;
  border-radius: var(--radius-md);
  margin-bottom: 2px;
}

.folder-tree :deep(.el-tree-node__content:hover) {
  background: var(--surface-container);
}

.folder-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--color-primary-light-9);
  color: var(--color-primary);
}

.folder-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.folder-node-icon {
  color: var(--color-primary);
  font-size: 16px;
  flex-shrink: 0;
}

.folder-node-name {
  flex: 1;
  font-size: var(--text-ui);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-node-count {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-right: 4px;
}

.folder-node-actions {
  opacity: 0;
  transition: opacity var(--duration-fast);
}

.folder-node:hover .folder-node-actions {
  opacity: 1;
}

.more-icon {
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast);
}

.more-icon:hover {
  background: var(--surface-active);
}

/* ========== 右侧内容区 ========== */
.content-main {
  flex: 1;
  min-width: 0;
}

/* Tabs */
.material-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: var(--surface-container);
  border-radius: var(--radius-lg);
  padding: 4px;
  width: fit-content;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.tab-btn:hover {
  color: var(--color-text-primary);
}

.tab-btn.active {
  background: var(--surface-card);
  color: var(--color-primary);
  box-shadow: var(--shadow-xs);
}

/* Toolbar */
.material-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
  min-width: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 面包屑 */
.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.breadcrumb-item {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast);
}

.breadcrumb-item:hover {
  color: var(--color-primary);
  background: var(--color-primary-light-9);
}

.breadcrumb-item:last-child {
  font-weight: 600;
  color: var(--color-text-primary);
}

.breadcrumb-sep {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* ========== 表格相关 ========== */
.table-loading { padding: 32px 24px; }

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-name {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-primary);
}

.file-size {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.file-desc {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 2px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--text-micro);
  font-weight: 600;
}

.folder-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-small);
  color: var(--color-primary);
  background: var(--color-primary-light-9);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.text-muted {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

.time-text {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--color-primary-light-9);
  border-top: 1px solid var(--outline-variant);
}

.batch-info {
  font-size: var(--text-ui);
  color: var(--color-primary);
  font-weight: 500;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--outline-variant);
}

.total-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

/* ========== 上传弹窗 ========== */
.upload-icon { color: var(--color-text-tertiary); margin-bottom: 8px; }
.upload-text { font-size: var(--text-body); color: var(--color-text-secondary); }
.upload-text em { color: var(--color-primary); font-style: normal; }
.upload-tip { font-size: var(--text-small); color: var(--color-text-tertiary); margin-top: 8px; }

/* ========== 详情抽屉 ========== */
.drawer-title {
  font-size: var(--text-heading-2);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-content { display: flex; flex-direction: column; gap: 20px; }
.detail-row { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: var(--text-small); color: var(--color-text-tertiary); font-weight: 500; }
.detail-value { font-size: var(--text-body); color: var(--color-text-primary); }
.detail-summary {
  font-size: var(--text-small);
  line-height: 1.6;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 12px;
  border-radius: var(--radius-md);
}
.detail-actions { display: flex; gap: 12px; margin-top: 8px; flex-wrap: wrap; }

.material-table :deep(.el-table__body tr) { cursor: pointer; }

/* ========== 调整表格复选框大小 ========== */
.material-table :deep(.el-checkbox__inner) {
  width: 18px;
  height: 18px;
}
.material-table :deep(.el-checkbox__inner::after) {
  height: 9px;
  left: 6px;
  top: 2px;
  width: 4px;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .material-layout {
    flex-direction: column;
  }

  .folder-sidebar {
    width: 100%;
    max-height: 300px;
    overflow-y: auto;
  }
}

@media (max-width: 767px) {
  .material-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    flex-wrap: wrap;
  }

  .table-footer {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .material-tabs {
    width: 100%;
  }

  .tab-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>

<style scoped>
/* 2026 desktop file-workbench refresh — loaded after legacy rules during incremental migration. */
.material-page {
  width: 100%;
  max-width: 1520px;
  margin: 0 auto;
}

.material-page :deep(.base-page-header) {
  margin-bottom: var(--space-5);
}

.material-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 0;
  min-height: calc(100vh - 184px);
  overflow: hidden;
  border: 1px solid var(--outline);
  border-radius: var(--radius-xl);
  background: var(--surface-card);
  box-shadow: var(--shadow-xs);
}

.folder-sidebar {
  width: auto;
  min-width: 0;
  overflow-y: auto;
  border: 0;
  border-right: 1px solid var(--outline-variant);
  border-radius: 0;
  background: var(--surface-container-low);
}

.folder-header {
  min-height: 58px;
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.folder-header-title {
  font-size: var(--text-small);
  font-weight: 650;
  letter-spacing: 0.01em;
}

.folder-root {
  min-height: 44px;
  margin: var(--space-3) var(--space-3) var(--space-1);
  padding: 0 var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
}

.folder-root:hover {
  background: color-mix(in srgb, var(--surface-card) 58%, transparent);
}

.folder-root.active {
  border-color: var(--outline);
  background: var(--surface-card);
  color: var(--color-primary);
  box-shadow: var(--shadow-xs);
}

.folder-root:focus-visible,
.tab-btn:focus-visible,
.row-more-button:focus-visible {
  outline: 2px solid var(--color-primary-ring);
  outline-offset: 2px;
}

.folder-count {
  min-width: 24px;
  padding: 1px 7px;
  background: var(--surface-container);
  text-align: center;
}

.folder-tree {
  padding: var(--space-1) var(--space-3) var(--space-4);
  background: transparent;
}

.folder-tree :deep(.el-tree-node__content) {
  height: 40px;
  padding-right: var(--space-1);
  border: 1px solid transparent;
}

.folder-tree :deep(.el-tree-node__content:hover) {
  background: color-mix(in srgb, var(--surface-card) 58%, transparent);
}

.folder-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  border-color: var(--outline);
  background: var(--surface-card);
  color: var(--color-primary);
  box-shadow: var(--shadow-xs);
}

.folder-node-icon {
  color: var(--color-text-tertiary);
}

.folder-tree :deep(.el-tree-node.is-current > .el-tree-node__content) .folder-node-icon {
  color: var(--color-primary);
}

.folder-node-actions {
  display: flex;
  align-items: center;
}

.content-main {
  min-width: 0;
  padding: var(--space-5) var(--space-6) var(--space-6);
  background: var(--surface-card);
}

.material-tabs {
  margin-bottom: var(--space-5);
  padding: 3px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.tab-btn {
  min-height: 34px;
  padding: 0 var(--space-4);
  border-radius: 8px;
  font-size: var(--text-small);
}

.tab-btn.active {
  background: var(--surface-card);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-xs);
}

.tab-btn.active .el-icon {
  color: var(--color-primary);
}

.material-toolbar {
  min-height: 40px;
  margin-bottom: var(--space-4);
  gap: var(--space-3);
}

.toolbar-right {
  gap: var(--space-2);
}

.breadcrumb-item {
  padding: var(--space-1) var(--space-2);
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.breadcrumb-item:hover {
  background: var(--surface-container-low);
  color: var(--color-primary);
}

.breadcrumb-item:last-child {
  color: var(--color-text-primary);
  font-weight: 600;
}

.material-list-panel {
  overflow: hidden;
  border-color: var(--outline);
  border-radius: var(--radius-lg);
  box-shadow: none;
}

.temporary-toolbar {
  justify-content: space-between;
}

.temporary-hint {
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.temporary-empty {
  padding: var(--space-10) 0;
}

.material-table :deep(.el-table__header-wrapper) {
  border-bottom: 1px solid var(--outline-variant);
}

.material-table :deep(th.el-table__cell) {
  height: 42px;
  background: var(--surface-container-low);
  color: var(--color-text-tertiary);
  font-size: var(--text-micro);
  letter-spacing: 0.02em;
  text-transform: none;
}

.material-table :deep(td.el-table__cell) {
  height: 60px;
  border-bottom-color: var(--outline-variant);
}

.material-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: color-mix(in srgb, var(--surface-container-low) 62%, transparent);
}

.file-cell {
  gap: var(--space-3);
}

.file-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
}

.file-name {
  font-size: var(--text-ui);
  font-weight: 560;
}

.file-size,
.file-desc {
  font-size: var(--text-micro);
}

.action-btns {
  justify-content: flex-end;
  gap: var(--space-1);
}

.row-more-button {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.row-more-button:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

.batch-bar {
  position: sticky;
  bottom: 0;
  z-index: var(--z-base);
  background: var(--bg-tag-green);
}

.table-footer {
  min-height: 58px;
  padding: var(--space-3) var(--space-5);
}

@media (max-width: 1100px) {
  .material-layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .content-main {
    padding-inline: var(--space-4);
  }
}

@media (max-width: 900px) {
  .material-layout {
    display: flex;
    flex-direction: column;
  }

  .folder-sidebar {
    width: 100%;
    max-height: 260px;
    border-right: 0;
    border-bottom: 1px solid var(--outline-variant);
  }
}

@media (max-width: 767px) {
  .content-main {
    padding: var(--space-4);
  }

  .material-toolbar,
  .toolbar-right {
    align-items: stretch;
  }

  .toolbar-right :deep(.el-input),
  .toolbar-right :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
