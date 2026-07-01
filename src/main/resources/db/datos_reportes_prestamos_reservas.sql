START TRANSACTION;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_seed_usuarios_reportes AS
SELECT id_usuario
FROM usuario
WHERE dni_usuario IN ('91010001', '91010002', '91010003', '91010004')
   OR email_usuario IN (
       'analopez@gmail.com',
       'brunosalas@gmail.com',
       'carladiaz@gmail.com',
       'diegoramos@gmail.com'
   );

DELETE s
FROM sancion s
INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = p.id_usuario;

DELETE i
FROM incidencia i
INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = p.id_usuario;

DELETE p
FROM prestamo p
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = p.id_usuario;

DELETE rd
FROM reserva_detalle rd
INNER JOIN reserva r ON r.id_reserva = rd.id_reserva
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = r.id_usuario;

DELETE r
FROM reserva r
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = r.id_usuario;

DELETE usuario
FROM usuario
INNER JOIN tmp_seed_usuarios_reportes u ON u.id_usuario = usuario.id_usuario;

DROP TEMPORARY TABLE tmp_seed_usuarios_reportes;

SET @rol_prestatario := (
    SELECT id_rol
    FROM rol
    WHERE nombre_rol = 'PRESTATARIO'
    LIMIT 1
);

INSERT INTO usuario
    (id_rol, nombre_usuario, apellido_usuario, dni_usuario, email_usuario, `contraseña_usuario`, estado_usuario)
VALUES
    (@rol_prestatario, 'Ana', 'Lopez', '91010001', 'analopez@gmail.com', 'clave123', 'ACTIVO'),
    (@rol_prestatario, 'Bruno', 'Salas', '91010002', 'brunosalas@gmail.com', 'clave123', 'ACTIVO'),
    (@rol_prestatario, 'Carla', 'Diaz', '91010003', 'carladiaz@gmail.com', 'clave123', 'ACTIVO'),
    (@rol_prestatario, 'Diego', 'Ramos', '91010004', 'diegoramos@gmail.com', 'clave123', 'ACTIVO');

SET @u_ana := (SELECT id_usuario FROM usuario WHERE email_usuario = 'analopez@gmail.com');
SET @u_bruno := (SELECT id_usuario FROM usuario WHERE email_usuario = 'brunosalas@gmail.com');
SET @u_carla := (SELECT id_usuario FROM usuario WHERE email_usuario = 'carladiaz@gmail.com');
SET @u_diego := (SELECT id_usuario FROM usuario WHERE email_usuario = 'diegoramos@gmail.com');

SET @libro_1 := 1;
SET @libro_2 := 3;
SET @libro_3 := 4;
SET @libro_4 := 5;
SET @libro_5 := 6;
SET @libro_6 := 7;
SET @libro_7 := 13;
SET @libro_8 := 16;

INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_ana, '2026-06-03', 'DEVUELTA');
SET @r_01 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_01, @libro_1, '2026-06-03', 'DEVUELTA', '10:00:00', '09:00:00', 1);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_ana, @libro_1, '2026-06-03', '09:00:00', '10:00:00', 'DEVUELTO', @r_01, '2026-06-03', '09:55:00');

INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_bruno, '2026-06-03', 'DEVUELTA');
SET @r_02 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_02, @libro_2, '2026-06-03', 'DEVUELTA', '12:00:00', '10:00:00', 2),
    (@r_02, @libro_3, '2026-06-03', 'DEVUELTA', '12:00:00', '10:00:00', 2);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_bruno, @libro_2, '2026-06-03', '10:00:00', '12:00:00', 'DEVUELTO_TARDIO', @r_02, '2026-06-03', '12:40:00'),
    (@u_bruno, @libro_3, '2026-06-03', '10:00:00', '12:00:00', 'DEVUELTO_TARDIO', @r_02, '2026-06-03', '12:40:00');
INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_carla, '2026-06-05', 'DEVUELTA');
SET @r_03 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_03, @libro_4, '2026-06-05', 'DEVUELTA', '17:00:00', '14:00:00', 3),
    (@r_03, @libro_5, '2026-06-05', 'DEVUELTA', '17:00:00', '14:00:00', 3),
    (@r_03, @libro_6, '2026-06-05', 'DEVUELTA', '17:00:00', '14:00:00', 3);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_carla, @libro_4, '2026-06-05', '14:00:00', '17:00:00', 'DEVUELTO', @r_03, '2026-06-05', '16:50:00'),
    (@u_carla, @libro_5, '2026-06-05', '14:00:00', '17:00:00', 'DEVUELTO', @r_03, '2026-06-05', '16:50:00'),
    (@u_carla, @libro_6, '2026-06-05', '14:00:00', '17:00:00', 'DEVUELTO', @r_03, '2026-06-05', '16:50:00');

INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_ana, '2026-06-10', 'DEVUELTA');
SET @r_04 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_04, @libro_1, '2026-06-10', 'DEVUELTA', '10:00:00', '08:00:00', 2),
    (@r_04, @libro_7, '2026-06-10', 'DEVUELTA', '10:00:00', '08:00:00', 2);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_ana, @libro_1, '2026-06-10', '08:00:00', '10:00:00', 'DEVUELTO', @r_04, '2026-06-10', '10:00:00'),
    (@u_ana, @libro_7, '2026-06-10', '08:00:00', '10:00:00', 'DEVUELTO', @r_04, '2026-06-10', '10:00:00');

INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_diego, '2026-06-10', 'DEVUELTA');
SET @r_05 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_05, @libro_8, '2026-06-10', 'DEVUELTA', '16:00:00', '15:00:00', 1);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_diego, @libro_8, '2026-06-10', '15:00:00', '16:00:00', 'DEVUELTO_TARDIO', @r_05, '2026-06-10', '17:15:00');
INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_bruno, '2026-06-14', 'DEVUELTA');
SET @r_06 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_06, @libro_3, '2026-06-14', 'DEVUELTA', '14:00:00', '11:00:00', 3),
    (@r_06, @libro_5, '2026-06-14', 'DEVUELTA', '14:00:00', '11:00:00', 3);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_bruno, @libro_3, '2026-06-14', '11:00:00', '14:00:00', 'DEVUELTO_TARDIO', @r_06, '2026-06-15', '09:10:00'),
    (@u_bruno, @libro_5, '2026-06-14', '11:00:00', '14:00:00', 'DEVUELTO_TARDIO', @r_06, '2026-06-15', '09:10:00');
INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_carla, '2026-06-18', 'DEVUELTA');
SET @r_07 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_07, @libro_7, '2026-06-18', 'DEVUELTA', '11:00:00', '09:00:00', 2),
    (@r_07, @libro_8, '2026-06-18', 'DEVUELTA', '11:00:00', '09:00:00', 2);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_carla, @libro_7, '2026-06-18', '09:00:00', '11:00:00', 'DEVUELTO', @r_07, '2026-06-18', '10:45:00'),
    (@u_carla, @libro_8, '2026-06-18', '09:00:00', '11:00:00', 'DEVUELTO', @r_07, '2026-06-18', '10:45:00');

INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_diego, '2026-06-18', 'DEVUELTA');
SET @r_08 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_08, @libro_1, '2026-06-18', 'DEVUELTA', '12:00:00', '09:00:00', 3),
    (@r_08, @libro_4, '2026-06-18', 'DEVUELTA', '12:00:00', '09:00:00', 3),
    (@r_08, @libro_7, '2026-06-18', 'DEVUELTA', '12:00:00', '09:00:00', 3);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_diego, @libro_1, '2026-06-18', '09:00:00', '12:00:00', 'DEVUELTO_TARDIO', @r_08, '2026-06-18', '12:30:00'),
    (@u_diego, @libro_4, '2026-06-18', '09:00:00', '12:00:00', 'DEVUELTO_TARDIO', @r_08, '2026-06-18', '12:30:00'),
    (@u_diego, @libro_7, '2026-06-18', '09:00:00', '12:00:00', 'DEVUELTO_TARDIO', @r_08, '2026-06-18', '12:30:00');
INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_ana, '2026-06-24', 'DEVUELTA');
SET @r_09 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_09, @libro_6, '2026-06-24', 'DEVUELTA', '17:00:00', '16:00:00', 1);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_ana, @libro_6, '2026-06-24', '16:00:00', '17:00:00', 'DEVUELTO_TARDIO', @r_09, '2026-06-24', '18:05:00');
INSERT INTO reserva (id_usuario, fecha_reserva, estado_reserva)
VALUES (@u_bruno, '2026-06-29', 'DEVUELTA');
SET @r_10 := LAST_INSERT_ID();
INSERT INTO reserva_detalle
    (id_reserva, id_libro, fecha_recojo_limite, estado_detalle_reserva, hora_recojo_limite, hora_reserva, horas_prestamo)
VALUES
    (@r_10, @libro_1, '2026-06-29', 'DEVUELTA', '12:00:00', '10:00:00', 2),
    (@r_10, @libro_2, '2026-06-29', 'DEVUELTA', '12:00:00', '10:00:00', 2);
INSERT INTO prestamo
    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin, estado_prestamo, id_reserva, fecha_devolucion_real, hora_devolucion_real)
VALUES
    (@u_bruno, @libro_1, '2026-06-29', '10:00:00', '12:00:00', 'DEVUELTO', @r_10, '2026-06-29', '11:50:00'),
    (@u_bruno, @libro_2, '2026-06-29', '10:00:00', '12:00:00', 'DEVUELTO', @r_10, '2026-06-29', '11:50:00');

COMMIT;
