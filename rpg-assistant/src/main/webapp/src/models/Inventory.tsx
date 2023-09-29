interface Item {
    name: string;
    damage: number;
    count: number;
}

interface Inventory {
    items: { [key: string]: Item }
}

export {type Inventory as default, type Item};