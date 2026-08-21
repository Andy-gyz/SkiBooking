WITH snow_alpine AS (
    INSERT INTO resorts (name, location, description, status)
    VALUES (
        'Snow Alpine Resort',
        'Victoria, Australia',
        'A fictional alpine resort offering one connected booking experience.',
        'ACTIVE'
    )
    ON CONFLICT (name) DO UPDATE SET
        location = EXCLUDED.location,
        description = EXCLUDED.description,
        status = EXCLUDED.status,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id
)
INSERT INTO products (resort_id, name, category, description, price, image_url, is_active)
SELECT snow_alpine.id, catalog.name, catalog.category, catalog.description, catalog.price, catalog.image_url, TRUE
FROM snow_alpine
CROSS JOIN (
    VALUES
        ('Daily Vehicle Entry', 'RESORT_ACCESS', 'One-day vehicle resort access.', 55.00, '/images/resort-entry-car.jpg'),
        ('Two-Day Vehicle Entry', 'RESORT_ACCESS', 'Vehicle access for two consecutive snow days.', 95.00, '/images/entry-two-day.jpg'),
        ('Oversize Vehicle Entry', 'RESORT_ACCESS', 'Daily entry for campervans, minibuses, and oversized vehicles.', 85.00, '/images/entry-oversize.jpg'),
        ('Season Parking Pass', 'RESORT_ACCESS', 'Reusable vehicle access throughout the 2026 snow season.', 420.00, '/images/entry-season.jpg'),
        ('Adult Full Day Lift Pass', 'LIFT_TICKET', 'Adult full-day lift access.', 135.00, '/images/lift-pass.jpg'),
        ('Child Full Day Lift Pass', 'LIFT_TICKET', 'Full-day lift access for guests aged 5 to 17.', 75.00, '/images/lift-child.jpg'),
        ('Afternoon Lift Pass', 'LIFT_TICKET', 'Lift access from 12:30 pm until close.', 95.00, '/images/lift-afternoon.jpg'),
        ('Beginner Area Lift Pass', 'LIFT_TICKET', 'All-day access to beginner lifts and learning terrain.', 68.00, '/images/lift-beginner.jpg'),
        ('Beginner Ski Lesson', 'LESSON', 'Two-hour beginner group ski lesson.', 120.00, '/images/ski-lesson.jpg'),
        ('Beginner Snowboard Lesson', 'LESSON', 'Two-hour beginner group snowboard lesson.', 125.00, '/images/lesson-snowboard.jpg'),
        ('Kids Snow Club', 'LESSON', 'A playful two-hour ski program for children aged 6 to 12.', 110.00, '/images/lesson-kids.jpg'),
        ('Private Ski Coaching', 'LESSON', 'A focused two-hour private session tailored to your goals.', 240.00, '/images/lesson-private.jpg'),
        ('Ski Package', 'RENTAL', 'Skis, boots, and poles.', 65.00, '/images/equipment-rental.jpg'),
        ('Snowboard Package', 'RENTAL', 'Snowboard, boots, and wrist guards.', 70.00, '/images/rental-snowboard.jpg'),
        ('Performance Ski Package', 'RENTAL', 'Premium skis, boots, and poles for confident riders.', 95.00, '/images/rental-performance.jpg'),
        ('Jacket and Pants Package', 'RENTAL', 'Waterproof outerwear for one snow day.', 45.00, '/images/rental-clothing.jpg')
) AS catalog(name, category, description, price, image_url)
ON CONFLICT (resort_id, name) DO NOTHING;
