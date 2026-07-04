<template>
  <div class="book-manage">
    <AdminSubHeader title="图书管理" />
    <div class="page-header">
      <div class="header-btns">
        <el-button type="primary" @click="openAddDialog">新增图书</el-button>
        <el-upload
          :action="batchUploadUrl"
          :headers="uploadHeader"
          :show-file-list="false"
          accept=".xlsx,.xls"
          @success="uploadSuccess"
          @error="uploadError"
        >
          <el-button>批量导入Excel</el-button>
        </el-upload>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <el-form inline :model="searchForm" class="search-form">
      <el-form-item label="书名">
        <el-input v-model="searchForm.title" placeholder="输入书名搜索" clearable />
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="searchForm.author" placeholder="作者名" clearable />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="searchForm.category" placeholder="全部" clearable>
          <el-option label="科幻" value="科幻" />
          <el-option label="文学" value="文学" />
          <el-option label="历史" value="历史" />
          <el-option label="科普" value="科普" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 图书表格 -->
    <el-table :data="bookList" border style="width:100%;margin:16px 0" v-loading="tableLoading">
      <el-table-column label="序号" type="index" width="70" align="center" />
      <el-table-column label="封面" width="100" align="center">
        <template #default="scope">
          <img
            v-if="scope.row.coverUrl || scope.row.coverImage"
            :src="scope.row.coverUrl || scope.row.coverImage"
            style="width:60px;height:80px;object-fit:cover;border-radius:4px"
          />
          <span v-else style="color:#999">无图</span>
        </template>
      </el-table-column>
      <el-table-column label="书名" prop="title" min-width="160" />
      <el-table-column label="作者" prop="author" width="120" />
      <el-table-column label="分类" width="100">
        <template #default="scope">
          {{ getCategoryName(scope.row.categoryId) }}
        </template>
      </el-table-column>
      <el-table-column label="字数(万)" prop="words" width="100" align="center" />
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === '已完结' ? 'success' : 'warning'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="平均分" prop="avgRating" width="90" align="center" />
      <el-table-column label="简介" show-overflow-tooltip min-width="220">
        <template #default="scope">
          {{ scope.row.description }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="scope">
          <el-button size="small" type="primary" text @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" text @click="handleDelete(scope.row.bookId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="[10, 20, 30, 40, 50]"
      @size-change="handleSizeChange"
      @current-change="loadBookList"
    />

    <!-- 新增/编辑图书弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '新增图书'" width="680">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col span="12">
            <el-form-item label="书名">
              <el-input v-model="form.title" placeholder="请输入书名" />
            </el-form-item>
          </el-col>
          <el-col span="12">
            <el-form-item label="作者">
              <el-input v-model="form.author" placeholder="作者名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" placeholder="选择分类">
                <el-option label="科幻" value="科幻" />
                <el-option label="文学" value="文学" />
                <el-option label="历史" value="历史" />
                <el-option label="科普" value="科普" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status">
                <el-option label="连载中" value="连载中" />
                <el-option label="已完结" value="已完结" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col span="12">
            <el-form-item label="字数(万)">
              <el-input-number v-model="form.words" :min="0" />
            </el-form-item>
          </el-col>
          <el-col span="12">
            <el-form-item label="封面链接">
              <el-input v-model="form.coverUrl" placeholder="图片URL" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-input v-model="form.tagsStr" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" :rows="4" placeholder="图书简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addBook, updateBook, deleteBook } from '@/api/admin'
import { getCategoryList, getBookList } from '@/api/book'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

let cancelFlag = false
const tableLoading = ref(false)
const submitLoading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const bookList = ref<any[]>([])

// 分类列表
const categories = ref<any[]>([])
const categoryMap = ref<Record<number, string>>({})

const defaultCover = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=book%20cover%20art%20minimalist%20design&image_size=square'

const demoBookList = [
  { bookId: 1, title: '三体', author: '刘慈欣', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=science%20fiction%20book%20cover%20space%20universe%20stars&image_size=square', categoryId: 1, category: '科幻', words: 51.5, status: '已完结', avgRating: 9.3, description: '地球文明向宇宙发出的第一声啼鸣，以太阳为中心，以光速向宇宙深处飞驰……' },
  { bookId: 2, title: '活着', author: '余华', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20literature%20book%20cover%20rural%20life%20emotional&image_size=square', categoryId: 2, category: '文学', words: 12.1, status: '已完结', avgRating: 9.4, description: '讲述了农村人福贵悲惨的人生遭遇。' },
  { bookId: 3, title: '百年孤独', author: '加西亚·马尔克斯', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=magic%20realism%20book%20cover%20tropical%20town%20butterflies&image_size=square', categoryId: 2, category: '文学', words: 36.0, status: '已完结', avgRating: 9.3, description: '描述了布恩迪亚家族七代人的传奇故事。' },
  { bookId: 4, title: '红楼梦', author: '曹雪芹', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20classical%20literature%20book%20cover%20traditional%20garden%20elegant&image_size=square', categoryId: 2, category: '文学', words: 73.0, status: '已完结', avgRating: 9.6, description: '中国古典四大名著之首。' },
  { bookId: 5, title: '围城', author: '钱钟书', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20chinese%20literature%20book%20cover%20city%20life%20intellectual&image_size=square', categoryId: 2, category: '文学', words: 25.0, status: '已完结', avgRating: 9.0, description: '人生的围城困境。' },
  { bookId: 6, title: '人类简史', author: '尤瓦尔·赫拉利', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=history%20book%20cover%20human%20evolution%20civilization&image_size=square', categoryId: 3, category: '历史', words: 28.0, status: '已完结', avgRating: 9.1, description: '重新审视人类历史。' },
  { bookId: 7, title: '平凡的世界', author: '路遥', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=rural%20china%20book%20cover%20farm%20life%20struggle&image_size=square', categoryId: 2, category: '文学', words: 104.0, status: '已完结', avgRating: 9.0, description: '平凡生活中的伟大。' },
  { bookId: 8, title: '小王子', author: '圣埃克苏佩里', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=fairy%20tale%20book%20cover%20little%20prince%20planet%20stars&image_size=square', categoryId: 4, category: '科普', words: 2.5, status: '已完结', avgRating: 9.1, description: '写给大人的童话。' },
  { bookId: 9, title: '明朝那些事儿', author: '当年明月', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20history%20book%20cover%20ming%20dynasty%20ancient%20palace&image_size=square', categoryId: 3, category: '历史', words: 50.0, status: '已完结', avgRating: 9.2, description: '通俗历史读物。' },
  { bookId: 10, title: '三体II：黑暗森林', author: '刘慈欣', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sci-fi%20book%20cover%20dark%20forest%20cosmos%20spaceship&image_size=square', categoryId: 1, category: '科幻', words: 43.0, status: '已完结', avgRating: 9.2, description: '宇宙社会学理论。' },
  { bookId: 11, title: '三体III：死神永生', author: '刘慈欣', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sci-fi%20book%20cover%20death%20star%20universe%20finale&image_size=square', categoryId: 1, category: '科幻', words: 51.0, status: '已完结', avgRating: 9.1, description: '三体系列的最终章。' },
  { bookId: 12, title: '1984', author: '乔治·奥威尔', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=dystopian%20book%20cover%20big%20brother%20surveillance&image_size=square', categoryId: 2, category: '文学', words: 18.0, status: '已完结', avgRating: 9.2, description: '反乌托邦经典。' },
  { bookId: 13, title: '思考，快与慢', author: '丹尼尔·卡尼曼', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=psychology%20book%20cover%20brain%20thinking%20cognitive&image_size=square', categoryId: 4, category: '科普', words: 40.0, status: '已完结', avgRating: 8.8, description: '诺贝尔经济学奖得主作品。' },
  { bookId: 14, title: '万历十五年', author: '黄仁宇', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20history%20book%20ming%20dynasty%20scholar&image_size=square', categoryId: 3, category: '历史', words: 18.0, status: '已完结', avgRating: 8.9, description: '大历史观代表作。' },
  { bookId: 15, title: '白夜行', author: '东野圭吾', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=mystery%20book%20cover%20dark%20night%20crime&image_size=square', categoryId: 2, category: '文学', words: 35.0, status: '已完结', avgRating: 9.1, description: '日本推理小说经典。' },
  { bookId: 16, title: '时间简史', author: '史蒂芬·霍金', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=physics%20book%20cover%20black%20hole%20time%20universe&image_size=square', categoryId: 4, category: '科普', words: 10.0, status: '已完结', avgRating: 8.7, description: '探索宇宙的奥秘。' },
  { bookId: 17, title: '三国演义', author: '罗贯中', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20classical%20war%20book%20cover%20ancient%20battle&image_size=square', categoryId: 3, category: '历史', words: 64.0, status: '已完结', avgRating: 9.3, description: '中国古典四大名著之一。' },
  { bookId: 18, title: '水浒传', author: '施耐庵', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20classical%20hero%20book%20cover%20outlaws%20mountain&image_size=square', categoryId: 3, category: '历史', words: 96.0, status: '已完结', avgRating: 9.1, description: '梁山好汉的故事。' },
  { bookId: 19, title: '西游记', author: '吴承恩', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20mythology%20book%20cover%20monkey%20king%20journey&image_size=square', categoryId: 4, category: '科普', words: 82.0, status: '已完结', avgRating: 9.2, description: '西天取经的传奇。' },
  { bookId: 20, title: '解忧杂货店', author: '东野圭吾', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=mystery%20book%20cover%20old%20shop%20warm%20light&image_size=square', categoryId: 2, category: '文学', words: 21.0, status: '已完结', avgRating: 8.8, description: '温暖治愈的故事。' },
  { bookId: 21, title: '枪炮、病菌与钢铁', author: '贾雷德·戴蒙德', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=history%20book%20cover%20civilization%20development%20evolution&image_size=square', categoryId: 3, category: '历史', words: 42.0, status: '已完结', avgRating: 8.9, description: '人类社会发展的奥秘。' },
  { bookId: 22, title: '浪潮之巅', author: '吴军', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=tech%20book%20cover%20internet%20silicon%20valley&image_size=square', categoryId: 4, category: '科普', words: 50.0, status: '已完结', avgRating: 8.7, description: '互联网时代的科技史。' },
  { bookId: 23, title: '挪威的森林', author: '村上春树', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=japanese%20literature%20book%20cover%20forest%20melancholy&image_size=square', categoryId: 2, category: '文学', words: 30.0, status: '已完结', avgRating: 8.5, description: '青春与死亡的故事。' },
  { bookId: 24, title: '自私的基因', author: '理查德·道金斯', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=biology%20book%20cover%20dna%20evolution%20genetics&image_size=square', categoryId: 4, category: '科普', words: 33.0, status: '已完结', avgRating: 9.0, description: '进化生物学经典。' },
  { bookId: 25, title: '沉默的大多数', author: '王小波', coverUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20essays%20book%20cover%20thought%20freedom&image_size=square', categoryId: 2, category: '文学', words: 15.0, status: '已完结', avgRating: 9.0, description: '知识分子的思考。' }
]

// 搜索条件
const searchForm = ref({
  title: '',
  author: '',
  category: ''
})

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const editBookId = ref<number | null>(null)
const form = ref({
  title: '',
  author: '',
  category: '',
  status: '连载中',
  words: 0,
  coverUrl: '',
  tagsStr: '',
  summary: ''
})

// 批量上传
const batchUploadUrl = '/api/v1/admin/books/batch'
const uploadHeader = ref({
  Authorization: ''
})

// 页面加载初始化token上传头
onMounted(() => {
  cancelFlag = false
  const token = localStorage.getItem('token') || ''
  uploadHeader.value.Authorization = `Bearer ${token}`
  loadCategories()
  loadBookList()
})
onUnmounted(() => cancelFlag = true)

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 0) {
      categories.value = res.data || []
      categoryMap.value = {}
      categories.value.forEach(cat => {
        categoryMap.value[cat.categoryId] = cat.name || cat.categoryId
      })
    }
  } catch (err) {
    console.error('加载分类列表失败', err)
  }
}

// 获取分类名称
const getCategoryName = (categoryId: number): string => {
  return categoryMap.value[categoryId] || '未分类'
}

const handleSearch = () => {
  pageNum.value = 1
  loadBookList()
}

const handleSizeChange = () => {
  pageNum.value = 1
  loadBookList()
}

// 加载图书列表
const loadBookList = async () => {
  tableLoading.value = true
  try {
    const params: Record<string, any> = {
      page: pageNum.value,
      size: pageSize.value
    }
    if (searchForm.value.title) params.keyword = searchForm.value.title
    if (searchForm.value.author) params.author = searchForm.value.author
    if (searchForm.value.category) params.category = searchForm.value.category
    
    const res = await getBookList(params)
    console.log('Book API response:', res)
    
    const responseData = res.data || res
    const items = responseData.records || responseData.items || responseData.list || []
    const totalCount = responseData.total || responseData.count || 0
    
    if (items.length > 0 || totalCount > 0) {
      bookList.value = items
      total.value = totalCount || items.length
    } else {
      bookList.value = []
      total.value = 0
    }
  } catch (err) {
    console.log('图书列表加载失败', err)
    bookList.value = []
    total.value = 0
  } finally {
    tableLoading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = { title: '', author: '', category: '' }
  pageNum.value = 1
  loadBookList()
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  editBookId.value = null
  form.value = {
    title: '',
    author: '',
    category: '',
    status: '连载中',
    words: 0,
    coverUrl: '',
    tagsStr: '',
    summary: ''
  }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row: any) => {
  isEdit.value = true
  editBookId.value = row.bookId
  form.value = {
    title: row.title,
    author: row.author,
    category: row.category,
    status: row.status,
    words: row.words,
    coverUrl: row.coverUrl || row.coverImage || '',
    tagsStr: Array.isArray(row.tags) ? row.tags.join(',') : '',
    summary: row.summary
  }
  dialogVisible.value = true
}

// 提交新增/编辑
const submitForm = async () => {
  const data = {
    ...form.value,
    tags: form.value.tagsStr.split(',').filter(i => i.trim())
  }
  submitLoading.value = true
  try {
    if (isEdit.value && editBookId.value) {
      await updateBook(editBookId.value, data)
      ElMessage.success('编辑成功')
    } else {
      await addBook(data)
      ElMessage.success('新增图书成功')
    }
    dialogVisible.value = false
    pageNum.value = 1
    loadBookList()
  } catch (err) {
    ElMessage.error('保存失败')
    console.error(err)
  } finally {
    submitLoading.value = false
  }
}

// 删除图书
const handleDelete = (bookId: number) => {
  ElMessageBox.confirm('确认删除该图书？删除后无法恢复', '警告', { type: 'warning' })
    .then(async () => {
      await deleteBook(bookId)
      ElMessage.success('删除成功')
      loadBookList()
    }).catch(() => {})
}

// 批量上传成功回调
const uploadSuccess = (res: any) => {
  ElMessage.success(`导入完成：成功${res.data.successCount}本，失败${res.data.failCount}本`)
  loadBookList()
}
const uploadError = () => {
  ElMessage.error('文件上传失败，请检查Excel格式或登录状态')
}
</script>

<style scoped lang="scss">
.book-manage {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 {
    margin: 0;
  }
  .header-btns {
    display: flex;
    gap: 12px;
  }
}
.search-form {
  background: #fff;
  padding: 16px;
  border-radius: 6px;
}
</style>