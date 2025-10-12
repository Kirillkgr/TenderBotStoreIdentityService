import React from 'react';

export default function IngredientTable({items = []}) {
    return (
        <div style={{padding: 24, fontFamily: 'Inter, sans-serif'}}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16}}>
                <div>
                    <h2 style={{margin: 0}}>Склад: Центральный</h2>
                    <div style={{color: '#6b7280', fontSize: 13}}>Управление ингредиентами</div>
                </div>

                <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                    <select style={{padding: 8, borderRadius: 8, border: '1px solid #e6e6e6'}}>
                        <option>Центральный</option>
                        <option>Кухня №2</option>
                    </select>
                    <button style={{
                        background: '#2563eb',
                        color: '#fff',
                        padding: '8px 12px',
                        borderRadius: 8,
                        border: 'none'
                    }}>Добавить ингредиент
                    </button>
                </div>
            </div>

            <table style={{width: '100%', borderCollapse: 'collapse'}}>
                <thead>
                <tr style={{background: '#fbfdff'}}>
                    <th style={{padding: 10, textAlign: 'left'}}>Название</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Кол-во</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Ед. изм.</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Срок годности</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Поставщик</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Категория</th>
                    <th style={{padding: 10, textAlign: 'left'}}>Действия</th>
                </tr>
                </thead>
                <tbody>
                {(items.length ? items : [{
                    name: 'Рис',
                    qty: 12,
                    unit: 'кг',
                    expires: '2025-11-01',
                    supplier: 'ООО "АзияФуд"',
                    category: 'Крупы',
                    low: true
                }, {
                    name: 'Молоко',
                    qty: 24,
                    unit: 'л',
                    expires: '2025-10-15',
                    supplier: 'ЗАО "МолКом"',
                    category: 'Молочные'
                }]).map((it, idx) => (
                    <tr key={idx}
                        style={{background: it.low ? '#fff3cd' : (new Date(it.expires) < new Date() ? '#f8d7da' : undefined)}}>
                        <td style={{padding: 10}}>{it.name}</td>
                        <td style={{padding: 10}}>{it.qty}</td>
                        <td style={{padding: 10}}>{it.unit}</td>
                        <td style={{padding: 10}}>{it.expires}</td>
                        <td style={{padding: 10}}>{it.supplier}</td>
                        <td style={{padding: 10}}>{it.category}</td>
                        <td style={{padding: 10}}>✏️ 🗑️</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    )
}
