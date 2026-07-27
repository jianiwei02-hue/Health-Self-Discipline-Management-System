import { reactive } from 'vue'

const eventBus = reactive({
    listeners: new Map(),

    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, [])
        }
        this.listeners.get(event).push(callback)
    },

    emit(event, data) {
        if (this.listeners.has(event)) {
            this.listeners.get(event).forEach(callback => callback(data))
        }
    },

    off(event, callback) {
        if (this.listeners.has(event)) {
            const filtered = this.listeners.get(event).filter(cb => cb !== callback)
            this.listeners.set(event, filtered)
        }
    }
})

export default eventBus