(function () {
    'use strict';

    var Vue = window.Vue;
    var LogicFlow = window.Core && (window.Core.LogicFlow || window.Core.default);

    if (!Vue) {
        return;
    }

    var app = Vue.createApp({
        data: function () {
            return {
                overview: { nodes: [], edges: [] },
                selectedNode: null,
                loading: true,
                error: '',
                graph: null
            };
        },
        computed: {
            readyCount: function () {
                return this.overview.nodes.filter(function (node) {
                    return node.status === 'READY';
                }).length;
            },
            propertySections: function () {
                var properties = this.selectedNode && this.selectedNode.properties;
                if (!properties) {
                    return [];
                }
                return Object.keys(properties).map(function (key) {
                    return {
                        key: key,
                        entries: Object.keys(properties[key] || {}).map(function (entryKey) {
                            return { key: entryKey, value: properties[key][entryKey] };
                        })
                    };
                }).filter(function (section) {
                    return section.entries.length > 0;
                });
            }
        },
        methods: {
            loadOverview: function () {
                var self = this;
                self.loading = true;
                self.error = '';
                fetch('/api/xline/overview', { headers: { Accept: 'application/json' } })
                    .then(function (response) {
                        if (!response.ok) {
                            throw new Error('HTTP ' + response.status);
                        }
                        return response.json();
                    })
                    .then(function (data) {
                        self.overview = {
                            nodes: Array.isArray(data.nodes) ? data.nodes : [],
                            edges: Array.isArray(data.edges) ? data.edges : []
                        };
                        self.retainSelection();
                        self.$nextTick(self.renderGraph);
                    })
                    .catch(function (error) {
                        self.error = '请求失败，请确认应用已启动（' + error.message + '）。';
                    })
                    .finally(function () {
                        self.loading = false;
                    });
            },
            retainSelection: function () {
                var self = this;
                if (!self.selectedNode) {
                    return;
                }
                self.selectedNode = self.overview.nodes.find(function (node) {
                    return node.name === self.selectedNode.name;
                }) || null;
            },
            selectNode: function (node) {
                this.selectedNode = node;
            },
            downstream: function (name) {
                return this.overview.edges.filter(function (edge) {
                    return edge.source === name;
                }).map(function (edge) {
                    return edge.target;
                }).join(', ');
            },
            formatValue: function (value) {
                if (value === null || value === undefined || value === '') {
                    return '—';
                }
                if (typeof value === 'object') {
                    return JSON.stringify(value);
                }
                return String(value);
            },
            renderGraph: function () {
                var container = document.getElementById('topology-graph');
                if (!container || !LogicFlow) {
                    return;
                }
                if (this.graph) {
                    this.graph.destroy();
                    this.graph = null;
                }
                if (!this.overview.nodes.length) {
                    return;
                }

                var nodes = this.overview.nodes.map(function (node, index) {
                    return {
                        id: 'node-' + node.name,
                        type: 'rect',
                        x: 150 + (index % 3) * 190,
                        y: 110 + Math.floor(index / 3) * 130,
                        width: 130,
                        height: 52,
                        text: node.name || 'unnamed',
                        properties: { status: node.status, nodeType: node.type || 'unknown' }
                    };
                });
                var edges = this.overview.edges.map(function (edge, index) {
                    return {
                        id: 'edge-' + index,
                        type: 'polyline',
                        sourceNodeId: 'node-' + edge.source,
                        targetNodeId: 'node-' + edge.target
                    };
                });
                var self = this;
                this.graph = new LogicFlow({
                    container: container,
                    grid: { size: 32, type: 'dot', visible: false },
                    background: { color: 'transparent' },
                    keyboard: { enabled: true },
                    stopScrollGraph: true,
                    stopZoomGraph: true,
                    isSilentMode: true
                });
                this.graph.setTheme({
                    rect: { radius: 6, stroke: '#4e8b92', strokeWidth: 1.5, fill: '#ffffff' },
                    polyline: { stroke: '#2267b1', strokeWidth: 1.5, offset: 28 },
                    nodeText: { color: '#17313a', fontSize: 12 },
                    arrow: { offset: 8, verticalLength: 5 }
                });
                this.graph.render({ nodes: nodes, edges: edges });
                this.graph.on('node:click', function (event) {
                    var node = self.overview.nodes.find(function (item) {
                        return 'node-' + item.name === event.data.node.id;
                    });
                    if (node) {
                        self.selectNode(node);
                    }
                });
            }
        },
        mounted: function () {
            this.loadOverview();
        }
    });

    app.mount('#app');
}());
