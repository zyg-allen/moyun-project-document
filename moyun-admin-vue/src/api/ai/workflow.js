import request from '@/utils/request'

export function listWorkflow(query) {
  return request({
    url: '/cms/ai/workflow/list',
    method: 'get',
    params: query
  })
}

export function getWorkflow(id) {
  return request({
    url: '/cms/ai/workflow/' + id,
    method: 'get'
  })
}

export function addWorkflow(data) {
  return request({
    url: '/cms/ai/workflow/create',
    method: 'post',
    data: data
  })
}

export function updateWorkflow(data) {
  return request({
    url: '/cms/ai/workflow/update',
    method: 'put',
    data: data
  })
}

export function delWorkflow(id) {
  return request({
    url: '/cms/ai/workflow/' + id,
    method: 'delete'
  })
}

export function executeWorkflow(id, input) {
  return request({
    url: '/cms/ai/workflow/' + id + '/execute',
    method: 'post',
    data: { input }
  })
}

export function listWorkflowExecutions(id, params) {
  return request({
    url: '/cms/ai/workflow/' + id + '/executions',
    method: 'get',
    params: params
  })
}

export function getWorkflowExecution(executionId) {
  return request({
    url: '/cms/ai/workflow/execution/' + executionId,
    method: 'get'
  })
}

export function toggleWorkflowStatus(id, enabled) {
  return request({
    url: '/cms/ai/workflow/' + id + '/toggle',
    method: 'post',
    data: { enabled }
  })
}

export function generateWorkflow(description) {
  return request({
    url: '/cms/ai/workflow-generator/generate',
    method: 'post',
    data: { description }
  })
}

export function copyWorkflow(id) {
  return request({
    url: '/cms/ai/workflow/' + id + '/copy',
    method: 'post'
  })
}
