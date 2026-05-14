package WebAplicacionesDesarrollo.demo.servicios;

import WebAplicacionesDesarrollo.demo.dtos.MateriaDTO;
import WebAplicacionesDesarrollo.demo.dtos.PruebaDTO;
import WebAplicacionesDesarrollo.demo.dtos.PruebaMapper;
import WebAplicacionesDesarrollo.demo.dtos.PruebaNuevaDTO;
import WebAplicacionesDesarrollo.demo.entidades.Materia;
import WebAplicacionesDesarrollo.demo.entidades.Prueba;
import WebAplicacionesDesarrollo.demo.entidades.Slot;
import WebAplicacionesDesarrollo.demo.excepcion.NoEncontradaException;
import WebAplicacionesDesarrollo.demo.repositorios.MateriaRepository;
import WebAplicacionesDesarrollo.demo.repositorios.PruebaRepository;
import WebAplicacionesDesarrollo.demo.repositorios.SlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PruebaServicio {

    private final PruebaRepository pruebaRepository;
    private final SlotRepository slotRepository;
    private final MateriaRepository materiaRepository;

    public PruebaServicio(PruebaRepository pruebaRepository, SlotRepository slotRepository, MateriaRepository materiaRepository) {
        this.pruebaRepository = pruebaRepository;
        this.slotRepository = slotRepository;
        this.materiaRepository = materiaRepository;
    }

    // 1. Método para listar con filtros
    public List<PruebaDTO> listarPruebas(Long idConvocatoria, Long idSlot) {
        List<Prueba> pruebas;

        if (idSlot != null) {
            // Si hay slot, filtramos por slot
            pruebas = pruebaRepository.findBySlot_Id(idSlot);
        } else if (idConvocatoria != null) {
            // Si no hay slot pero hay convocatoria, usamos @Query con JOIN FETCH
            pruebas = pruebaRepository.findBySlot_Convocatoria_IdConvocatoria(idConvocatoria);
        } else {
            // Si no hay filtros, traemos las que no estén eliminadas
            pruebas = pruebaRepository.findByEliminadaFalse();
        }

        return pruebas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // 2. Método para obtener por ID
    public PruebaDTO obtenerPruebaPorId(Long id) {
        Prueba prueba = pruebaRepository.findById(id)
                .filter(p -> !p.isEliminada())
                .orElseThrow(() -> new NoEncontradaException());
        return PruebaMapper.toDTO(pruebaRepository.save(prueba));
    }

    // 3. Método para crear
    public PruebaDTO crearPrueba(PruebaNuevaDTO dto) {
        Slot slot = slotRepository.findById(dto.getIdSlot().getId())
                .orElseThrow(() -> new NoEncontradaException("Slot no encontrado"));

        Materia materia = materiaRepository.findById(dto.getIdMateria().getId())
                .orElseThrow(() -> new NoEncontradaException("Materia no encontrada"));

        Prueba nuevaPrueba = new Prueba();
        nuevaPrueba.setSlot(slot);
        nuevaPrueba.setMateria(materia);
        nuevaPrueba.setEliminada(false);

        return PruebaMapper.toDTO(pruebaRepository.save(nuevaPrueba));
    }

    // 4. Método para actualizar
    public PruebaDTO actualizarPrueba(Long id, PruebaNuevaDTO dto) {
        Prueba pruebaExistente = pruebaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradaException());

        Slot slot = slotRepository.findById(dto.getIdSlot().getId())
                .orElseThrow(() -> new NoEncontradaException());

        Materia materia = materiaRepository.findById(dto.getIdMateria().getId())
                .orElseThrow(() -> new NoEncontradaException());

        pruebaExistente.setSlot(slot);
        pruebaExistente.setMateria(materia);

        return PruebaMapper.toDTO(pruebaRepository.save(pruebaExistente));
    }

    // 5. Método de borrado lógico
    public void borradoLogicoPrueba(Long id) {
        Prueba prueba = pruebaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradaException());
        prueba.setEliminada(true);
        pruebaRepository.save(prueba);
    }

    // Método auxiliar para convertir Entidad a DTO
    private PruebaDTO convertirADTO(Prueba prueba) {
        PruebaDTO dto = new PruebaDTO();
        dto.setId(prueba.getId());
        dto.setEliminada(prueba.isEliminada());

        // Convertimos la Entidad Slot a SlotDTO usando SlotMapper
        dto.setSlot(WebAplicacionesDesarrollo.demo.dtos.SlotMapper.toDTO(prueba.getSlot()));

        // Convertimos la Entidad Materia a MateriaDTO manualmente
        MateriaDTO materiaDTO = new MateriaDTO();
        materiaDTO.setId(prueba.getMateria().getId());
        materiaDTO.setNombre(prueba.getMateria().getNombre());
        materiaDTO.setEliminada(prueba.getMateria().isEliminada());

        // Asignamos la materiaDTO ya rellena al PruebaDTO final
        dto.setMateria(materiaDTO);

        return dto;
    }
}