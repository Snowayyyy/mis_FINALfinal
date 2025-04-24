package com.mis.api;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.mis.db.AnimalDAO;
import com.mis.db.BoxDAO;
import com.mis.db.OwnerDAO;
import com.mis.db.TreatmentDAO;
import com.mis.model.Animal;
import com.mis.model.Box;
import com.mis.model.BoxStatus;
import com.mis.model.Owner;
import com.mis.model.Treatment;
import com.mis.model.TreatmentType;

/**
 * API controller for Animal operations
 */
public class AnimalController {
    private AnimalDAO animalDAO;
    private BoxDAO boxDAO;
    private OwnerDAO ownerDAO;
    private TreatmentDAO treatmentDAO;
    
    public AnimalController() {
        this.animalDAO = new AnimalDAO();
        this.boxDAO = new BoxDAO();
        this.ownerDAO = new OwnerDAO();
        this.treatmentDAO = new TreatmentDAO();
    }
    
    /**
     * Get all animals
     */
    public List<Animal> getAllAnimals() throws SQLException {
        return animalDAO.getAll();
    }
    
    /**
     * Get animal by ID
     */
    public Animal getAnimalById(int id) throws SQLException {
        return animalDAO.getById(id);
    }
    
    /**
     * Create a new animal
     */
    public Animal createAnimal(String name, String species, String breed, 
                              LocalDate birthDate, String gender) throws SQLException {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setSpecies(species);
        animal.setBreed(breed);
        animal.setBirthDate(birthDate);
        animal.setGender(gender);
        
        int animalId = animalDAO.save(animal);
        return animalDAO.getById(animalId);
    }
    
    /**
     * Update an animal
     */
    public boolean updateAnimal(Animal animal) throws SQLException {
        return animalDAO.update(animal);
    }
    
    /**
     * Delete an animal
     */
    public boolean deleteAnimal(int id) throws SQLException {
        return animalDAO.delete(id);
    }
    
    /**
     * Assign an animal to an owner
     */
    public boolean assignOwner(int animalId, int ownerId) throws SQLException {
        Animal animal = animalDAO.getById(animalId);
        Owner owner = ownerDAO.getById(ownerId);
        
        if (animal != null && owner != null) {
            animal.setOwner(owner);
            owner.addAnimal(animal);
            return animalDAO.update(animal);
        }
        
        return false;
    }
    
    /**
     * Assign an animal to a box
     */
    public boolean assignBox(int animalId, int boxId) throws SQLException {
        Animal animal = animalDAO.getById(animalId);
        Box box = boxDAO.getById(boxId);
        
        if (animal != null && box != null && box.isAvailable()) {
            // If animal is already in a box, release it first
            if (animal.getBox() != null) {
                Box oldBox = animal.getBox();
                oldBox.releaseAnimal();
                boxDAO.update(oldBox);
            }
            
            // Assign to new box
            box.assignAnimal(animal);
            boxDAO.update(box);
            return animalDAO.update(animal);
        }
        
        return false;
    }
    
    /**
     * Release animal from current box
     */
    public boolean releaseFromBox(int animalId) throws SQLException {
        Animal animal = animalDAO.getById(animalId);
        
        if (animal != null && animal.getBox() != null) {
            Box box = animal.getBox();
            box.releaseAnimal();
            boxDAO.update(box);
            return animalDAO.update(animal);
        }
        
        return false;
    }
    
    /**
     * Add a treatment for an animal
     */
    public Treatment addTreatment(int animalId, TreatmentType type, String name, 
                                 String description, LocalDate nextDueDate) throws SQLException {
        Animal animal = animalDAO.getById(animalId);
        
        if (animal != null) {
            Treatment treatment = new Treatment();
            treatment.setType(type);
            treatment.setName(name);
            treatment.setDescription(description);
            treatment.setNextDueDate(nextDueDate);
            treatment.setAdministered(false);
            
            int treatmentId = treatmentDAO.save(animalId, treatment);
            Treatment savedTreatment = treatmentDAO.getById(treatmentId);
            animal.addTreatment(savedTreatment);
            
            return savedTreatment;
        }
        
        return null;
    }
    
    /**
     * Record that a treatment has been administered
     */
    public boolean administerTreatment(int treatmentId, LocalDate nextDueDate) throws SQLException {
        Treatment treatment = treatmentDAO.getById(treatmentId);
        
        if (treatment != null) {
            treatment.administer();
            treatment.setNextDueDate(nextDueDate);
            return treatmentDAO.update(treatment);
        }
        
        return false;
    }
    
    /**
     * Get animals with overdue treatments
     */
    public List<Animal> getAnimalsWithOverdueTreatments() throws SQLException {
        return animalDAO.getAllWithOverdueTreatments();
    }
} 